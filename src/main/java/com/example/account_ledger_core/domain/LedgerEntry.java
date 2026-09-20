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
            if (entryId == null || entryId.isBlank() || accountId == null || currency == null || amount == null
                    || direction == null || entryType == null || bookingDay == null || valueDate == null
                    || causeEventId == null || causeEventId.isBlank() || !amount.isPositive()
                    || amount.currency() != currency || !compatible(direction, entryType)
                    || (reversalOfEntryId != null && reversalOfEntryId.isBlank())) {
                throw new IllegalArgumentException("invalid ledger entry");
            }
        }

        private static boolean compatible(EntryDirection direction, LedgerEntryType type) {
            boolean credit = direction == EntryDirection.CREDIT;
            return switch (type) {
                case CREDIT, INSTALLMENT, REVERSAL_POSTING, FEE_REVERSAL, INTEREST_CAPITALIZATION -> credit;
                case DEBIT, SETTLEMENT_DEBIT, OVERDRAFT_FEE -> !credit;
            };
        }

    public Money signedAmount() {
        return direction == EntryDirection.CREDIT ? amount : amount.negate();
    }
}
