package com.example.account_ledger_core.domain;

public record ReversalEvent(
        String eventId,
        int sequencePosition,
        AccountId accountId,
        String originalEventId,
        CurrencyCode currency,
        SimulationDay bookingDay,
        SimulationDay valueDate) implements LedgerEvent {
    public ReversalEvent {
        if (eventId == null || eventId.isBlank() || accountId == null
                || originalEventId == null || originalEventId.isBlank() || currency == null
                || bookingDay == null || valueDate == null)
            throw new IllegalArgumentException("invalid reversal event");
    }
}
