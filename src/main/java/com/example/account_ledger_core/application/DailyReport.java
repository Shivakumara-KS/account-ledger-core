package com.example.account_ledger_core.application;

import com.example.account_ledger_core.domain.*;
import java.util.List;
import java.util.Map;

public record DailyReport(
        SimulationDay day,
        Map<AccountId, Money> closingBalances,
        List<Authorization> authorizations,
        List<LedgerEntry> feeEntries,
        List<LedgerEntry> feeReversals,
        List<ProcessingError> errors,
        Map<AccountId, Money> interestAccruals) {}
