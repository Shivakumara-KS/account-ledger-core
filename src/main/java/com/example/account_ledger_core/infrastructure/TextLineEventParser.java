package com.example.account_ledger_core.infrastructure;

import com.example.account_ledger_core.domain.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TextLineEventParser {
    private static final Pattern HEADER = Pattern.compile("^(E\\d+)\\s+—\\s+Day\\s+(\\d+)\\s+—\\s+([A-Z_]+)\\s+—\\s+(.+?)\\s+—\\s+value_date\\s+Day\\s+(\\d+)$");
    private static final Pattern POSTING = Pattern.compile("^([A-Za-z0-9-]+)\\s+([A-Z]{3})\\s+([\\d,]+(?:\\.\\d+)?)$");
    private static final Pattern AUTHORIZATION = Pattern.compile("^([A-Za-z0-9-]+)\\s+([A-Za-z0-9-]+)\\s+hold\\s+([A-Z]{3})\\s+([\\d,]+(?:\\.\\d+)?)$");
    private static final Pattern SETTLEMENT = Pattern.compile("^([A-Za-z0-9-]+)\\s+([A-Za-z0-9-]+)\\s+settles\\s+for\\s+([A-Z]{3})\\s+([\\d,]+(?:\\.\\d+)?)$");
    private static final Pattern REVERSAL = Pattern.compile("^([A-Za-z0-9-]+)\\s+reverses\\s+(E\\d+)$");
    private static final Pattern INSTALLMENTS = Pattern.compile("^([A-Za-z0-9-]+)\\s+([A-Z]{3})\\s+([\\d,]+(?:\\.\\d+)?),\\s+posted\\s+as\\s+([A-Za-z]+|\\d+)\\s+equal\\s+instalments$");

    public List<LedgerEvent> load(Path path) {
        Objects.requireNonNull(path, "path");
        try {
            return parse(Files.readAllLines(path, StandardCharsets.UTF_8));
        } catch (java.io.IOException e) {
            throw new IllegalArgumentException("Unable to read event file '" + path + "'", e);
        }
    }

    public List<LedgerEvent> parse(List<String> lines) {
        Objects.requireNonNull(lines, "lines");
        List<LedgerEvent> events = new ArrayList<>();
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index).trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            try {
                events.add(parseLine(line, events.size() + 1, events));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid event at line " + (index + 1) + ": " + e.getMessage(), e);
            }
        }
        return List.copyOf(events);
    }

    private LedgerEvent parseLine(String line, int sequencePosition, List<LedgerEvent> priorEvents) {
        Matcher header = HEADER.matcher(line);
        if (!header.matches()) {
            throw new IllegalArgumentException("expected: E1 — Day 1 — CREDIT — details — value_date Day 1");
        }
        String eventId = header.group(1);
        SimulationDay bookingDay = day(header.group(2), "booking day");
        String type = header.group(3);
        String details = header.group(4);
        SimulationDay valueDate = day(header.group(5), "value date");

        return switch (type) {
            case "CREDIT" -> credit(eventId, sequencePosition, details, bookingDay, valueDate);
            case "DEBIT" -> debit(eventId, sequencePosition, details, bookingDay, valueDate);
            case "AUTHORIZATION" -> authorization(eventId, sequencePosition, details, bookingDay, valueDate);
            case "SETTLEMENT" -> settlement(eventId, sequencePosition, details, bookingDay, valueDate);
            case "REVERSAL" -> reversal(eventId, sequencePosition, details, bookingDay, valueDate, priorEvents);
            default -> throw new IllegalArgumentException("unsupported event type '" + type + "'");
        };
    }

    private LedgerEvent credit(String eventId, int sequence, String details, SimulationDay booking, SimulationDay value) {
        Matcher installments = INSTALLMENTS.matcher(details);
        if (installments.matches()) {
            return new InstallmentCreditEvent(eventId, sequence, account(installments.group(1)),
                    money(installments.group(2), installments.group(3)), installmentCount(installments.group(4)),
                    booking, value);
        }
        Matcher posting = POSTING.matcher(details);
        if (!posting.matches()) throw new IllegalArgumentException("invalid CREDIT details");
        return new CreditEvent(eventId, sequence, account(posting.group(1)),
                money(posting.group(2), posting.group(3)), booking, value);
    }

    private LedgerEvent debit(String eventId, int sequence, String details, SimulationDay booking, SimulationDay value) {
        Matcher posting = POSTING.matcher(details);
        if (!posting.matches()) throw new IllegalArgumentException("invalid DEBIT details");
        return new DebitEvent(eventId, sequence, account(posting.group(1)),
                money(posting.group(2), posting.group(3)), booking, value);
    }

    private LedgerEvent authorization(String eventId, int sequence, String details,
                                      SimulationDay booking, SimulationDay value) {
        Matcher match = AUTHORIZATION.matcher(details);
        if (!match.matches()) throw new IllegalArgumentException("invalid AUTHORIZATION details");
        return new AuthorizationEvent(eventId, sequence, account(match.group(1)),
                new AuthorizationId(match.group(2)), money(match.group(3), match.group(4)), booking, value);
    }

    private LedgerEvent settlement(String eventId, int sequence, String details,
                                   SimulationDay booking, SimulationDay value) {
        Matcher match = SETTLEMENT.matcher(details);
        if (!match.matches()) throw new IllegalArgumentException("invalid SETTLEMENT details");
        return new SettlementEvent(eventId, sequence, account(match.group(1)),
                new AuthorizationId(match.group(2)), money(match.group(3), match.group(4)), booking, value);
    }

    private LedgerEvent reversal(String eventId, int sequence, String details,
                                 SimulationDay booking, SimulationDay value, List<LedgerEvent> priorEvents) {
        Matcher match = REVERSAL.matcher(details);
        if (!match.matches()) throw new IllegalArgumentException("invalid REVERSAL details");
        LedgerEvent original = priorEvents.stream()
                .filter(event -> event.eventId().equals(match.group(2))).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("reversal target must precede the reversal"));
        return new ReversalEvent(eventId, sequence, account(match.group(1)), match.group(2),
                original.currency(), booking, value);
    }

    private Money money(String currency, String amount) {
        try {
            return Money.of(currency(currency), new BigDecimal(amount.replace(",", "")));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("amount must be a decimal number", e);
        }
    }

    private CurrencyCode currency(String value) {
        try {
            return CurrencyCode.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("currency must be AED or BHD", e);
        }
    }

    private SimulationDay day(String value, String field) {
        int number = integer(value, field);
        if (number < 1 || number > 6) throw new IllegalArgumentException(field + " must be Day 1 through Day 6");
        return SimulationDay.values()[number - 1];
    }

    private AccountId account(String value) {
        return new AccountId(value);
    }

    private int integer(String value, String field) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(field + " must be an integer", e);
        }
    }

    private int installmentCount(String value) {
        return switch (value.toLowerCase()) {
            case "one" -> 1;
            case "two" -> 2;
            case "three" -> 3;
            case "four" -> 4;
            case "five" -> 5;
            default -> integer(value, "installments");
        };
    }
}
