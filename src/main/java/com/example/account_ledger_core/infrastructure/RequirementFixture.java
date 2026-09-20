package com.example.account_ledger_core.infrastructure;

import com.example.account_ledger_core.domain.*;
import java.util.List;

public final class RequirementFixture {
    private RequirementFixture() {}
    public static List<LedgerEvent> events() {
        AccountId aed = new AccountId("ACC-001");
        AccountId bhd = new AccountId("ACC-002");
        return List.of(
                new CreditEvent("E1", 1, aed, Money.of(CurrencyCode.AED, "1200.00"), SimulationDay.DAY1, SimulationDay.DAY1),
                new DebitEvent("E2", 2, aed, Money.of(CurrencyCode.AED, "950.00"), SimulationDay.DAY1, SimulationDay.DAY1),
                new AuthorizationEvent("E3", 3, aed, new AuthorizationId("Auth-A"), Money.of(CurrencyCode.AED, "200.00"), SimulationDay.DAY2, SimulationDay.DAY2),
                new CreditEvent("E4", 4, aed, Money.of(CurrencyCode.AED, "400.00"), SimulationDay.DAY3, SimulationDay.DAY3),
                new SettlementEvent("E5", 5, aed, new AuthorizationId("Auth-A"), Money.of(CurrencyCode.AED, "185.00"), SimulationDay.DAY4, SimulationDay.DAY4),
                new SettlementEvent("E6", 6, aed, new AuthorizationId("Auth-Z"), Money.of(CurrencyCode.AED, "180.00"), SimulationDay.DAY4, SimulationDay.DAY4),
                new DebitEvent("E7", 7, aed, Money.of(CurrencyCode.AED, "620.00"), SimulationDay.DAY5, SimulationDay.DAY2),
                new AuthorizationEvent("E8", 8, aed, new AuthorizationId("Auth-B"), Money.of(CurrencyCode.AED, "90.00"), SimulationDay.DAY5, SimulationDay.DAY5),
                new ReversalEvent("E9", 9, aed, "E7", CurrencyCode.AED, SimulationDay.DAY6, SimulationDay.DAY2),
                new InstallmentCreditEvent("E10", 10, bhd, Money.of(CurrencyCode.BHD, "10.000"), 3, SimulationDay.DAY5, SimulationDay.DAY5)
        );
    }
}
