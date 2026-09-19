package com.example.account_ledger_core.application;

import com.example.account_ledger_core.domain.LedgerEvent;
import java.util.List;

public interface EventJournal {
    void receive(LedgerEvent event);
    void accept(LedgerEvent event);
    void reject(LedgerEvent event);
    List<LedgerEvent> received();
    List<LedgerEvent> accepted();
    List<LedgerEvent> rejected();
}
