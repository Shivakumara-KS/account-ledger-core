package com.example.account_ledger_core.domain;

public record SettlementEvent(
        String eventId,
        int sequencePosition,
        AccountId accountId,
        AuthorizationId authorizationId,
        Money amount,
        SimulationDay bookingDay,
        SimulationDay valueDate) implements LedgerEvent {
    public CurrencyCode currency() {
        return amount.currency();
    }
}
