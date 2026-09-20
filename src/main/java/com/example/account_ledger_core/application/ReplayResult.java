package com.example.account_ledger_core.application;

import com.example.account_ledger_core.domain.*;
import java.util.List;
import java.util.Map;

public record ReplayResult(List<LedgerEvent> eventHistory, List<LedgerEvent> acceptedEvents,
                           List<LedgerEvent> rejectedIngestionAttempts, List<LedgerEntry> ledgerEntries,
                           List<Authorization> authorizations, List<ProcessingError> processingErrors,
                           List<LedgerEntry> feeAssessments, List<LedgerEntry> feeReversals,
                           List<DailyInterestAccrual> dailyInterestAccruals,
                           Map<SimulationDay, DailyReport> dailyReports,
                           Map<AccountId, Money> finalBalances) {
    public ReplayResult {
        eventHistory = List.copyOf(eventHistory);
        acceptedEvents = List.copyOf(acceptedEvents);
        rejectedIngestionAttempts = List.copyOf(rejectedIngestionAttempts);
        ledgerEntries = List.copyOf(ledgerEntries);
        authorizations = List.copyOf(authorizations);
        processingErrors = List.copyOf(processingErrors);
        feeAssessments = List.copyOf(feeAssessments);
        feeReversals = List.copyOf(feeReversals);
        dailyInterestAccruals = List.copyOf(dailyInterestAccruals);
        dailyReports = Map.copyOf(dailyReports);
        finalBalances = Map.copyOf(finalBalances);
    }
}
