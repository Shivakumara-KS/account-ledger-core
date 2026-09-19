package com.example.account_ledger_core.infrastructure;

import com.example.account_ledger_core.domain.LedgerEntry;
import com.example.account_ledger_core.application.LedgerJournal;

import java.util.*;

public final class InMemoryLedgerJournal implements LedgerJournal {
    private final List<LedgerEntry> entries = new ArrayList<>();

    public void append(LedgerEntry entry) {
        entries.add(Objects.requireNonNull(entry));
    }

    public List<LedgerEntry> entries() {
        return List.copyOf(entries);
    }
}
