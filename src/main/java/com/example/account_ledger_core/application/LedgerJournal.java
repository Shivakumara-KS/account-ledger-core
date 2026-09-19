package com.example.account_ledger_core.application;

import com.example.account_ledger_core.domain.LedgerEntry;
import java.util.List;

public interface LedgerJournal {
    void append(LedgerEntry entry);
    List<LedgerEntry> entries();
}
