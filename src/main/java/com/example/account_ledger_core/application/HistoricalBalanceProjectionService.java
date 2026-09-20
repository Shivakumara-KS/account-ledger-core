package com.example.account_ledger_core.application;

import com.example.account_ledger_core.domain.*;
import java.util.*;

public final class HistoricalBalanceProjectionService {
    public Money balance(AccountId account, SimulationDay valueDate, SimulationDay asOf,
                         List<LedgerEntry> entries, CurrencyCode currency) {
        return balance(account, valueDate, asOf, entries, currency, ProjectionMode.OPERATING_BALANCE);
    }

    public Money balance(AccountId account, SimulationDay valueDate, SimulationDay asOf,
                         List<LedgerEntry> entries, CurrencyCode currency, ProjectionMode mode) {
        Money result = Money.of(currency, "0");
        for (LedgerEntry entry : entries) {
            if (entry.accountId().equals(account)
                    && entry.bookingDay().number() <= asOf.number()
                    && entry.valueDate().number() <= valueDate.number()
                    && includes(entry.entryType(), mode)) {
                result = result.add(entry.signedAmount());
            }
        }
        return result;
    }

    private boolean includes(LedgerEntryType type, ProjectionMode mode) {
        return switch (mode) {
            case ALL_ENTRIES -> true;
            case BOOKED_BALANCE -> type != LedgerEntryType.INTEREST_CAPITALIZATION;
            case OPERATING_BALANCE -> type != LedgerEntryType.OVERDRAFT_FEE
                    && type != LedgerEntryType.FEE_REVERSAL
                    && type != LedgerEntryType.INTEREST_CAPITALIZATION;
        };
    }

    public Map<SimulationDay, Money> finalBalances(AccountId account, CurrencyCode currency,
                                                    List<LedgerEntry> entries) {
        EnumMap<SimulationDay, Money> result = new EnumMap<>(SimulationDay.class);
        for (SimulationDay day : SimulationDay.values()) {
            result.put(day, balance(account, day, SimulationDay.DAY6, entries, currency,
                    ProjectionMode.BOOKED_BALANCE));
        }
        return Collections.unmodifiableMap(result);
    }
}
