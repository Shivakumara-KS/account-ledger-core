package com.example.account_ledger_core.domain;

public record ReversalEvent(
        String eventId,
        int sequencePosition,
        AccountId accountId,
        String originalEventId,
        CurrencyCode currency,
        SimulationDay bookingDay,
        SimulationDay valueDate) implements LedgerEvent {
}
