package com.example.account_ledger_core.domain;

public sealed interface LedgerEvent permits CreditEvent, DebitEvent, AuthorizationEvent,
        SettlementEvent, ReversalEvent, InstallmentCreditEvent {
    String eventId();
    int sequencePosition();
    AccountId accountId();
    CurrencyCode currency();
    SimulationDay bookingDay();
    SimulationDay valueDate();
}
