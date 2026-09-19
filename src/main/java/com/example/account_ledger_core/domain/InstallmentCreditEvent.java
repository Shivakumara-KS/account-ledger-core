package com.example.account_ledger_core.domain;
public record InstallmentCreditEvent(
        String eventId,
        int sequencePosition,
        AccountId accountId,
        Money totalAmount,
        int installmentCount,
        SimulationDay bookingDay,
        SimulationDay valueDate) implements LedgerEvent {
    public InstallmentCreditEvent {
        if (installmentCount < 1)
            throw new IllegalArgumentException("installment count");
    }
    public CurrencyCode currency() {
        return totalAmount.currency();
    }
}
