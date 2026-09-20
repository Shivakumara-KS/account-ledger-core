package com.example.account_ledger_core.infrastructure;

import com.example.account_ledger_core.application.*;
import com.example.account_ledger_core.domain.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public final class ConsoleReplayRunner implements CommandLineRunner {
    private final ReplayEngine engine = new ReplayEngine();
    @Override public void run(String... args) {
        ReplayResult result = engine.replay(RequirementFixture.events());
        System.out.println("account-ledger-core replay");
        for (SimulationDay day : SimulationDay.values()) {
            DailyReport report = result.dailyReports().get(day);
            System.out.println(day + " balances=" + report.closingBalances()
                    + " fees=" + report.feeEntries().size()
                    + " feeReversals=" + report.feeReversals().size()
                    + " authorizations=" + report.authorizations()
                    + " errors=" + report.errors().size());
        }
        System.out.println("final=" + result.finalBalances());
    }
}
