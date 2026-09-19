package com.example.account_ledger_core.domain;
public record AuthorizationEvent(
        String eventId,
        int sequencePosition,
        AccountId accountId,
        AuthorizationId authorizationId,
        Money holdAmount, SimulationDay bookingDay,
        SimulationDay valueDate
) implements LedgerEvent {
    public CurrencyCode currency() {
        return holdAmount.currency();
    }
}
