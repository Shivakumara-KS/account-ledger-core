package com.example.account_ledger_core.domain;

public record CreditEvent(
        String eventId,
        int sequencePosition,
        AccountId accountId,
        Money amount,
        SimulationDay bookingDay,
        SimulationDay valueDate) implements LedgerEvent {
    public CreditEvent {
        if (amount.currency() == null)
            throw new IllegalArgumentException("amount");
    }

    public CurrencyCode currency() {
        return amount.currency();
    }
}
