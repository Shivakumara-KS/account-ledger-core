package com.example.account_ledger_core;

import com.example.account_ledger_core.domain.*;
import com.example.account_ledger_core.infrastructure.RequirementFixture;
import com.example.account_ledger_core.application.ReplayEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import static org.junit.jupiter.api.Assertions.assertEquals;

@EnabledIfSystemProperty(named = "intentional.failure", matches = "true")
class IntentionalFailureTest {
    @Test void rejectedThreeEqualInstallmentsAssumption() {
        // Deliberately fails in the intentional-failure profile: 3.334 x 3 creates 10.002 BHD.
        var actual = new ReplayEngine().replay(RequirementFixture.events()).ledgerEntries().stream()
                .filter(e -> e.entryType() == LedgerEntryType.INSTALLMENT)
                .map(e -> e.amount().amount()).toList();
        assertEquals(java.util.List.of(new java.math.BigDecimal("3.334"),
                new java.math.BigDecimal("3.334"), new java.math.BigDecimal("3.334")), actual);
    }
}
