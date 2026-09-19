package com.example.account_ledger_core.domain;
public record DebitEvent(
        String eventId,
        int sequencePosition,
        AccountId accountId,
        Money amount,
        SimulationDay bookingDay,
        SimulationDay valueDate) implements LedgerEvent {
    public CurrencyCode currency() {
        return amount.currency();
    }
}
