package com.example.account_ledger_core.domain;

public record LedgerEntry(
        String entryId,
        AccountId accountId,
        CurrencyCode currency,
        Money amount,
        EntryDirection direction,
        LedgerEntryType entryType,
        SimulationDay bookingDay,
        SimulationDay valueDate,
        String causeEventId,
        String reversalOfEntryId) {
    public LedgerEntry {
        if (amount.currency() != currency)
            throw new IllegalArgumentException("entry currency mismatch");
    }

    public Money signedAmount() {
        return direction == EntryDirection.CREDIT ? amount : amount.negate();
    }
}
