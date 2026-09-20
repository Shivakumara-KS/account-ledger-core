package com.example.account_ledger_core.domain;
public record AuthorizationEvent(
        String eventId,
        int sequencePosition,
        AccountId accountId,
        AuthorizationId authorizationId,
        Money holdAmount, SimulationDay bookingDay,
        SimulationDay valueDate
) implements LedgerEvent {
    public AuthorizationEvent {
        if (eventId == null || eventId.isBlank() || accountId == null || authorizationId == null
                || holdAmount == null || !holdAmount.isPositive() || bookingDay == null || valueDate == null)
            throw new IllegalArgumentException("invalid authorization event");
    }

    public CurrencyCode currency() {
        return holdAmount.currency();
    }
}
