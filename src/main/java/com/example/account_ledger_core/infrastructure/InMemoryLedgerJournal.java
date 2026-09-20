package com.example.account_ledger_core.infrastructure;

import com.example.account_ledger_core.domain.LedgerEntry;
import com.example.account_ledger_core.application.LedgerJournal;

import java.util.*;

public final class InMemoryLedgerJournal implements LedgerJournal {
    private final List<LedgerEntry> entries = new ArrayList<>();

    public synchronized void append(LedgerEntry entry) {
        LedgerEntry checked = Objects.requireNonNull(entry);
        if (entries.stream().anyMatch(existing -> existing.entryId().equals(checked.entryId()))) {
            throw new IllegalArgumentException("duplicate ledger entry id: " + checked.entryId());
        }
        entries.add(checked);
    }

    public synchronized List<LedgerEntry> entries() {
        return List.copyOf(entries);
    }
}
