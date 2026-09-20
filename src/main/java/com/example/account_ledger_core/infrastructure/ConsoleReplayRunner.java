package com.example.account_ledger_core.infrastructure;

import com.example.account_ledger_core.application.*;
import com.example.account_ledger_core.domain.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;

@Component
public final class ConsoleReplayRunner implements CommandLineRunner {
    private final ReplayEngine engine;
    private final TextLineEventParser textLineEventParser;

    public ConsoleReplayRunner() {
        this(new ReplayEngine(), new TextLineEventParser());
    }

    public ConsoleReplayRunner(ReplayEngine engine, TextLineEventParser textLineEventParser) {
        this.engine = engine;
        this.textLineEventParser = textLineEventParser;
    }

    @Override public void run(String... args) {
        ReplayResult result = engine.replay(eventsFor(args));
        System.out.println("account-ledger-core replay");
        for (SimulationDay day : SimulationDay.values()) {
            DailyReport report = result.dailyReports().get(day);
            System.out.println(day + " balances=" + report.closingBalances()
                    + " fees=" + report.feeEntries()
                    + " feeReversals=" + report.feeReversals()
                    + " authorizations=" + report.authorizations()
                    + " errors=" + report.errors());
        }
        System.out.println("final=" + result.finalBalances());
    }

    public List<LedgerEvent> eventsFor(String... args) {
        Path textPath = textEventPath(args);
        if (textPath != null) {
            return textLineEventParser.load(textPath);
        }
        return RequirementFixture.events();
    }

    private Path textEventPath(String... args) {
        for (int index = 0; index < args.length; index++) {
            String argument = args[index];
            if (argument.startsWith("--file=")) {
                String value = argument.substring("--file=".length()).trim();
                if (value.isEmpty()) {
                    throw new IllegalArgumentException("--file requires a filesystem path");
                }
                return resolvePath(value);
            }
            if (argument.equals("--file")) {
                if (index + 1 >= args.length || args[index + 1].isBlank()) {
                    throw new IllegalArgumentException("--file requires a filesystem path");
                }
                return resolvePath(args[++index]);
            }
        }
        return null;
    }

    private Path resolvePath(String value) {
        return Path.of(value).toAbsolutePath().normalize();
    }
}
