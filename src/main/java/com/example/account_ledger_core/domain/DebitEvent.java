package com.example.account_ledger_core.domain;
public record DebitEvent(
        String eventId,
        int sequencePosition,
        AccountId accountId,
        Money amount,
        SimulationDay bookingDay,
        SimulationDay valueDate) implements LedgerEvent {
    public DebitEvent {
        if (eventId == null || eventId.isBlank() || accountId == null || amount == null
                || !amount.isPositive() || bookingDay == null || valueDate == null)
            throw new IllegalArgumentException("invalid debit event");
    }

    public CurrencyCode currency() {
        return amount.currency();
    }
}
