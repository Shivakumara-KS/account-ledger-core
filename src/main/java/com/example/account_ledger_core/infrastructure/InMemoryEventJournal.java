package com.example.account_ledger_core.infrastructure;

import com.example.account_ledger_core.domain.LedgerEvent;
import com.example.account_ledger_core.application.EventJournal;

import java.util.*;

public final class InMemoryEventJournal implements EventJournal {
    private final List<LedgerEvent> received = new ArrayList<>();
    private final List<LedgerEvent> accepted = new ArrayList<>();
    private final List<LedgerEvent> rejected = new ArrayList<>();

    public void receive(LedgerEvent event) {
        received.add(event);
    }

    public void accept(LedgerEvent event) {
        accepted.add(event);
    }

    public void reject(LedgerEvent event) {
        rejected.add(event);
    }

    public List<LedgerEvent> received() {
        return List.copyOf(received);
    }

    public List<LedgerEvent> accepted() {
        return List.copyOf(accepted);
    }

    public List<LedgerEvent> rejected() {
        return List.copyOf(rejected);
    }
}
