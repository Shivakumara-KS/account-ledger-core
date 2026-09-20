package com.example.account_ledger_core.application;

import com.example.account_ledger_core.domain.*;
import java.math.*;
import java.util.*;

public final class ReplayEngine {
    private static final Money OVERDRAFT_FEE = Money.of(CurrencyCode.AED, "25.00");
    private static final BigDecimal DAILY_RATE = new BigDecimal("0.0004");
    private final HistoricalBalanceProjectionService projection = new HistoricalBalanceProjectionService();

    public ReplayResult replay(List<LedgerEvent> input) {
        Objects.requireNonNull(input, "events");
        List<LedgerEvent> history = List.copyOf(input);
        List<LedgerEvent> accepted = new ArrayList<>(), rejected = new ArrayList<>();
        List<LedgerEntry> entries = new ArrayList<>();
        List<ProcessingError> errors = new ArrayList<>();
        Map<String, LedgerEvent> byId = new LinkedHashMap<>();
        Map<String, Authorization> auths = new LinkedHashMap<>();
        Set<String> reversedEvents = new HashSet<>();
        int expectedPosition = 1;

        for (LedgerEvent event : history) {
            if (byId.containsKey(event.eventId())) {
                reject(event, ErrorCode.DUPLICATE_EVENT_ID, "Event id already received", errors, rejected);
                continue;
            }
            byId.put(event.eventId(), event);
            if (event.sequencePosition() != expectedPosition++) {
                reject(event, ErrorCode.INVALID_EVENT_ORDER, "Sequence position is not the supplied stream order",
                        errors, rejected);
                continue;
            }
            accepted.add(event);
            if (event instanceof CreditEvent e) {
                entries.add(entry(e.eventId(), e.accountId(), e.amount(), EntryDirection.CREDIT,
                        LedgerEntryType.CREDIT, e.bookingDay(), e.valueDate(), e.eventId(), null, entries.size()));
            } else if (event instanceof DebitEvent e) {
                entries.add(entry(e.eventId(), e.accountId(), e.amount(), EntryDirection.DEBIT,
                        LedgerEntryType.DEBIT, e.bookingDay(), e.valueDate(), e.eventId(), null, entries.size()));
            } else if (event instanceof AuthorizationEvent e) {
                Money current = projection.balance(e.accountId(), e.bookingDay(), e.bookingDay(), entries, e.currency());
                Money holds = activeHolds(e.accountId(), e.bookingDay(), auths.values(), e.currency());
                Money available = current.subtract(holds).subtract(e.holdAmount());
                Authorization authorization = new Authorization(e.authorizationId(), e.accountId(), e.currency(),
                        e.holdAmount(), e.bookingDay(),
                        available.isNegative() ? AuthorizationState.REJECTED : AuthorizationState.APPROVED);
                auths.put(e.authorizationId().value(), authorization);
                if (authorization.state() == AuthorizationState.REJECTED)
                    errors.add(new ProcessingError(e.eventId(), ErrorCode.INSUFFICIENT_AVAILABLE_BALANCE,
                            "Available balance would be negative"));
            } else if (event instanceof SettlementEvent e) {
                Authorization auth = auths.get(e.authorizationId().value());
                if (auth == null) {
                    rejectAccepted(e, ErrorCode.SETTLEMENT_AUTH_NOT_FOUND, "Authorization does not exist", errors);
                } else if (!auth.accountId().equals(e.accountId())) {
                    rejectAccepted(e, ErrorCode.ACCOUNT_MISMATCH, "Authorization account differs", errors);
                } else if (auth.currency() != e.currency()) {
                    rejectAccepted(e, ErrorCode.CURRENCY_MISMATCH, "Authorization currency differs", errors);
                } else if (auth.state() != AuthorizationState.APPROVED) {
                    rejectAccepted(e, ErrorCode.SETTLEMENT_AUTH_NOT_APPROVED, "Authorization is not approved", errors);
                } else {
                    entries.add(entry(e.eventId(), e.accountId(), e.amount(), EntryDirection.DEBIT,
                            LedgerEntryType.SETTLEMENT_DEBIT, e.bookingDay(), e.valueDate(), e.eventId(), null, entries.size()));
                    auths.put(auth.authorizationId().value(), auth.withState(AuthorizationState.SETTLED));
                }
            } else if (event instanceof ReversalEvent e) {
                LedgerEvent original = byId.get(e.originalEventId());
                if (original == null) {
                    rejectAccepted(e, ErrorCode.EVENT_NOT_FOUND, "Reversal target does not exist", errors);
                } else if (reversedEvents.contains(e.originalEventId())) {
                    rejectAccepted(e, ErrorCode.EVENT_ALREADY_REVERSED, "Reversal target already reversed", errors);
                } else if (!(original instanceof DebitEvent)) {
                    rejectAccepted(e, ErrorCode.INVALID_REVERSAL_TARGET, "Only debit postings are reversible", errors);
                } else {
                    DebitEvent debit = (DebitEvent) original;
                    if (!debit.accountId().equals(e.accountId())) {
                        rejectAccepted(e, ErrorCode.ACCOUNT_MISMATCH, "Reversal account differs", errors);
                    } else if (debit.currency() != e.currency()) {
                        rejectAccepted(e, ErrorCode.CURRENCY_MISMATCH, "Reversal currency differs", errors);
                    } else {
                        entries.add(entry(e.eventId(), e.accountId(), debit.amount(), EntryDirection.CREDIT,
                                LedgerEntryType.REVERSAL_POSTING, e.bookingDay(), e.valueDate(), e.eventId(),
                                debit.eventId(), entries.size()));
                        reversedEvents.add(e.originalEventId());
                    }
                }
            } else if (event instanceof InstallmentCreditEvent e) {
                appendInstallments(e, entries);
            }
            // A newly discovered backdated debit can create fees immediately, while later events can reconcile them.
            reconcileFees(event.bookingDay(), entries);
        }

        // E9 makes all historical balances positive; compensate earlier fee assessments without deleting them.
        reconcileFees(SimulationDay.DAY6, entries);
        List<DailyInterestAccrual> accruals = calculateAccruals(entries);
        Map<AccountId, Money> interest = totals(accruals);
        for (Map.Entry<AccountId, Money> item : interest.entrySet()) {
            if (!item.getValue().isZero()) {
                entries.add(entry("INTEREST-" + item.getKey(), item.getKey(), item.getValue(), EntryDirection.CREDIT,
                        LedgerEntryType.INTEREST_CAPITALIZATION, SimulationDay.DAY6, SimulationDay.DAY6,
                        "INTEREST-DAY6", null, entries.size()));
            }
        }

        List<LedgerEntry> feeAssessments = entries.stream().filter(e -> e.entryType() == LedgerEntryType.OVERDRAFT_FEE).toList();
        List<LedgerEntry> feeReversals = entries.stream().filter(e -> e.entryType() == LedgerEntryType.FEE_REVERSAL).toList();
        Map<AccountId, Money> finalBalances = finalBalances(entries);
        Map<SimulationDay, DailyReport> reports = reports(entries, auths, errors, accepted,
                feeAssessments, interest);
        return new ReplayResult(history, accepted, rejected, entries, auths.values().stream().toList(), errors,
                feeAssessments, feeReversals, accruals, reports, finalBalances);
    }

    private void appendInstallments(InstallmentCreditEvent e, List<LedgerEntry> entries) {
        BigDecimal unit = e.totalAmount().amount().divide(BigDecimal.valueOf(e.installmentCount()),
                e.currency().scale(), RoundingMode.DOWN);
        BigDecimal allocated = BigDecimal.ZERO;
        for (int i = 0; i < e.installmentCount(); i++) {
            BigDecimal amount = i == 0 ? e.totalAmount().amount().subtract(unit.multiply(BigDecimal.valueOf(e.installmentCount() - 1))) : unit;
            allocated = allocated.add(amount);
            Money installment = Money.of(e.currency(), amount);
            entries.add(entry(e.eventId() + "-" + (i + 1), e.accountId(), installment, EntryDirection.CREDIT,
                    LedgerEntryType.INSTALLMENT, e.bookingDay(), e.valueDate(), e.eventId(), null, entries.size()));
        }
        if (allocated.compareTo(e.totalAmount().amount()) != 0) throw new IllegalStateException("installment conservation");
    }

    private void reconcileFees(SimulationDay asOf, List<LedgerEntry> entries) {
        Set<String> assessed = new HashSet<>();
        Set<String> reversed = new HashSet<>();
        for (LedgerEntry e : entries) {
            if (e.entryType() == LedgerEntryType.OVERDRAFT_FEE) assessed.add(e.accountId() + "|" + e.valueDate());
            if (e.entryType() == LedgerEntryType.FEE_REVERSAL) reversed.add(e.reversalOfEntryId());
        }
        for (AccountId account : accounts(entries)) {
            CurrencyCode currency = currencyOf(account, entries);
            if (currency != CurrencyCode.AED) continue;
            for (SimulationDay day : SimulationDay.values()) {
                if (day.number() > asOf.number()) continue;
                String key = account + "|" + day;
                Money raw = baseBalance(account, day, asOf, entries, currency);
                if (raw.isNegative() && !assessed.contains(key)) {
                    LedgerEntry fee = entry("FEE-" + account + "-" + day, account, OVERDRAFT_FEE,
                            EntryDirection.DEBIT, LedgerEntryType.OVERDRAFT_FEE, asOf, day,
                            "FEE-RECONCILIATION-" + asOf, null, entries.size());
                    entries.add(fee);
                    assessed.add(key);
                } else if (!raw.isNegative() && assessed.contains(key)) {
                    for (LedgerEntry fee : entries.stream().filter(x -> x.entryType() == LedgerEntryType.OVERDRAFT_FEE
                            && x.accountId().equals(account) && x.valueDate() == day).toList()) {
                        if (!reversed.contains(fee.entryId())) {
                            entries.add(entry("FEE-REV-" + fee.entryId(), account, fee.amount(), EntryDirection.CREDIT,
                                    LedgerEntryType.FEE_REVERSAL, asOf, day, "FEE-RECONCILIATION-" + asOf,
                                    fee.entryId(), entries.size()));
                            reversed.add(fee.entryId());
                        }
                    }
                }
            }
        }
    }

    private Money baseBalance(AccountId account, SimulationDay day, SimulationDay asOf,
                               List<LedgerEntry> entries, CurrencyCode currency) {
        Money result = Money.of(currency, "0");
        for (LedgerEntry e : entries) {
            if (e.accountId().equals(account) && e.bookingDay().number() <= asOf.number()
                    && e.valueDate().number() <= day.number()
                    && e.entryType() != LedgerEntryType.OVERDRAFT_FEE
                    && e.entryType() != LedgerEntryType.FEE_REVERSAL) result = result.add(e.signedAmount());
        }
        return result;
    }

    private List<DailyInterestAccrual> calculateAccruals(List<LedgerEntry> entries) {
        List<DailyInterestAccrual> result = new ArrayList<>();
        for (AccountId account : accounts(entries)) {
            CurrencyCode currency = currencyOf(account, entries);
            for (SimulationDay day : SimulationDay.values()) {
                Money balance = baseFinalBalance(account, day, entries, currency);
                Money accrual = balance.isPositive() ? balance.multiply(DAILY_RATE, currency.scale())
                        : Money.of(currency, "0");
                result.add(new DailyInterestAccrual(account, day, accrual));
            }
        }
        return result;
    }

    private Map<AccountId, Money> totals(List<DailyInterestAccrual> accruals) {
        Map<AccountId, Money> result = new LinkedHashMap<>();
        for (DailyInterestAccrual accrual : accruals) {
            result.merge(accrual.accountId(), accrual.amount(), Money::add);
        }
        return result;
    }

    private Money baseFinalBalance(AccountId account, SimulationDay day, List<LedgerEntry> entries, CurrencyCode c) {
        Money result = Money.of(c, "0");
        for (LedgerEntry e : entries) if (e.accountId().equals(account)
                && e.valueDate().number() <= day.number()
                && e.entryType() != LedgerEntryType.INTEREST_CAPITALIZATION) result = result.add(e.signedAmount());
        return result;
    }

    private Map<AccountId, Money> finalBalances(List<LedgerEntry> entries) {
        Map<AccountId, Money> result = new LinkedHashMap<>();
        for (AccountId account : accounts(entries)) {
            CurrencyCode c = currencyOf(account, entries);
            Money balance = baseFinalBalance(account, SimulationDay.DAY6, entries, c);
            Money interest = Money.of(c, "0");
            for (LedgerEntry e : entries) if (e.entryType() == LedgerEntryType.INTEREST_CAPITALIZATION
                    && e.accountId().equals(account)) interest = interest.add(e.signedAmount());
            result.put(account, balance.add(interest));
        }
        return result;
    }

    private Map<SimulationDay, DailyReport> reports(List<LedgerEntry> entries, Map<String, Authorization> auths,
            List<ProcessingError> errors, List<LedgerEvent> accepted, List<LedgerEntry> fees,
            Map<AccountId, Money> interest) {
        Map<SimulationDay, DailyReport> result = new EnumMap<>(SimulationDay.class);
        for (SimulationDay day : SimulationDay.values()) {
            Map<AccountId, Money> balances = new LinkedHashMap<>();
            for (AccountId account : accounts(entries)) {
                balances.put(account, balanceAt(account, day, day, entries, currencyOf(account, entries)));
            }
            List<ProcessingError> dayErrors = errors.stream().filter(error -> accepted.stream()
                    .filter(event -> event.eventId().equals(error.eventId()))
                    .anyMatch(event -> event.bookingDay().number() == day.number())).toList();
            List<Authorization> dayAuthorizations = auths.values().stream()
                    .filter(a -> a.bookingDay().number() <= day.number())
                    .map(a -> authorizationAt(a, day, accepted)).toList();
            result.put(day, new DailyReport(day, balances, dayAuthorizations,
                    fees.stream().filter(f -> f.bookingDay().number() == day.number()).toList(),
                    entries.stream().filter(f -> f.entryType() == LedgerEntryType.FEE_REVERSAL
                            && f.bookingDay().number() == day.number()).toList(),
                    dayErrors, interest));
        }
        return result;
    }

    private Money balanceAt(AccountId account, SimulationDay valueDate, SimulationDay asOf,
                            List<LedgerEntry> entries, CurrencyCode currency) {
        Money result = Money.of(currency, "0");
        for (LedgerEntry entry : entries) {
            if (entry.accountId().equals(account)
                    && entry.bookingDay().number() <= asOf.number()
                    && entry.valueDate().number() <= valueDate.number()) {
                result = result.add(entry.signedAmount());
            }
        }
        return result;
    }

    private Authorization authorizationAt(Authorization authorization, SimulationDay day,
                                          List<LedgerEvent> accepted) {
        if (authorization.state() != AuthorizationState.SETTLED) return authorization;
        boolean settled = accepted.stream().filter(e -> e instanceof SettlementEvent)
                .map(e -> (SettlementEvent) e)
                .anyMatch(e -> e.authorizationId().equals(authorization.authorizationId())
                        && e.bookingDay().number() <= day.number());
        return settled ? authorization : authorization.withState(AuthorizationState.APPROVED);
    }

    private Set<AccountId> accounts(List<LedgerEntry> entries) {
        Set<AccountId> result = new LinkedHashSet<>();
        entries.forEach(e -> result.add(e.accountId()));
        return result;
    }
    private CurrencyCode currencyOf(AccountId account, List<LedgerEntry> entries) {
        return entries.stream().filter(e -> e.accountId().equals(account)).findFirst().orElseThrow().currency();
    }
    private Money activeHolds(AccountId account, SimulationDay day, Collection<Authorization> auths, CurrencyCode c) {
        Money result = Money.of(c, "0");
        for (Authorization a : auths) if (a.accountId().equals(account) && a.currency() == c
                && a.state() == AuthorizationState.APPROVED && a.bookingDay().number() <= day.number()) result = result.add(a.holdAmount());
        return result;
    }
    private LedgerEntry entry(String id, AccountId account, Money amount, EntryDirection direction, LedgerEntryType type,
                              SimulationDay booking, SimulationDay value, String cause, String reversal, int index) {
        return new LedgerEntry(id, account, amount.currency(), amount, direction, type, booking, value, cause, reversal);
    }
    private void reject(LedgerEvent event, ErrorCode code, String message, List<ProcessingError> errors, List<LedgerEvent> rejected) {
        rejected.add(event);
        errors.add(new ProcessingError(event.eventId(), code, message));
    }
    private void rejectAccepted(LedgerEvent event, ErrorCode code, String message, List<ProcessingError> errors) {
        errors.add(new ProcessingError(event.eventId(), code, message));
    }
}
