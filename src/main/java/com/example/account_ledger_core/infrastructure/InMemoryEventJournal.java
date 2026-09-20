package com.example.account_ledger_core.infrastructure;

import com.example.account_ledger_core.domain.LedgerEvent;
import com.example.account_ledger_core.application.EventJournal;

import java.util.*;

public final class InMemoryEventJournal implements EventJournal {
    private final List<LedgerEvent> received = new ArrayList<>();
    private final List<LedgerEvent> accepted = new ArrayList<>();
    private final List<LedgerEvent> rejected = new ArrayList<>();
    private final Set<String> acceptedIds = new HashSet<>();

    public synchronized void receive(LedgerEvent event) {
        received.add(Objects.requireNonNull(event));
    }

    public synchronized void accept(LedgerEvent event) {
        LedgerEvent checked = Objects.requireNonNull(event);
        if (!acceptedIds.add(checked.eventId())) {
            throw new IllegalArgumentException("duplicate accepted event id: " + checked.eventId());
        }
        accepted.add(checked);
    }

    public synchronized void reject(LedgerEvent event) {
        rejected.add(Objects.requireNonNull(event));
    }

    public synchronized List<LedgerEvent> received() {
        return List.copyOf(received);
    }

    public synchronized List<LedgerEvent> accepted() {
        return List.copyOf(accepted);
    }

    public synchronized List<LedgerEvent> rejected() {
        return List.copyOf(rejected);
    }
}
