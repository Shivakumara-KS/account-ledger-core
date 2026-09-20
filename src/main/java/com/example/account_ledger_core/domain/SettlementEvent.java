package com.example.account_ledger_core.domain;

public record SettlementEvent(
        String eventId,
        int sequencePosition,
        AccountId accountId,
        AuthorizationId authorizationId,
        Money amount,
        SimulationDay bookingDay,
        SimulationDay valueDate) implements LedgerEvent {
    public SettlementEvent {
        if (eventId == null || eventId.isBlank() || accountId == null || authorizationId == null
                || amount == null || !amount.isPositive() || bookingDay == null || valueDate == null)
            throw new IllegalArgumentException("invalid settlement event");
    }

    public CurrencyCode currency() {
        return amount.currency();
    }
}
