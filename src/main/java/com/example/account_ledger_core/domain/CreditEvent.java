package com.example.account_ledger_core.domain;

public record CreditEvent(
        String eventId,
        int sequencePosition,
        AccountId accountId,
        Money amount,
        SimulationDay bookingDay,
        SimulationDay valueDate) implements LedgerEvent {
    public CreditEvent {
        validate(eventId, accountId, amount, bookingDay, valueDate);
    }

    private static void validate(String eventId, AccountId accountId, Money amount,
                                 SimulationDay bookingDay, SimulationDay valueDate) {
        if (eventId == null || eventId.isBlank() || accountId == null || amount == null
                || !amount.isPositive() || bookingDay == null || valueDate == null)
            throw new IllegalArgumentException("invalid credit event");
    }

    public CurrencyCode currency() {
        return amount.currency();
    }
}
