package com.example.account_ledger_core.infrastructure;

import com.example.account_ledger_core.domain.*;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TextLineEventParserTest {
    private static final Path FIXTURES = Path.of("src/test/resources/custom-events");

    @Test
    void loadsPipeDelimitedTextInOrderAndSupportsComments() {
        List<LedgerEvent> events = new TextLineEventParser().load(
                FIXTURES.resolve("05-complex-multi-account.txt"));

        assertEquals(List.of("E1", "E2", "E3", "E4", "E5", "E6"),
                events.stream().map(LedgerEvent::eventId).toList());
        assertInstanceOf(InstallmentCreditEvent.class, events.get(1));
        assertEquals(CurrencyCode.BHD, events.get(1).currency());
    }

    @Test
    void rejectsMalformedTextWithLineNumber() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new TextLineEventParser().parse(List.of("E1 — Day 1 — CREDIT — broken — value_date Day 1")));

        assertTrue(exception.getMessage().contains("line 1"));
        assertTrue(exception.getMessage().contains("CREDIT"));
    }

    @Test
    void runnerUsesRequirementFixtureByDefaultAndLoadsTextFile() {
        ConsoleReplayRunner runner = new ConsoleReplayRunner();
        Path file = FIXTURES.resolve("01-simple-credit.txt");

        assertEquals(RequirementFixture.events(), runner.eventsFor());
        assertEquals(List.of("E1"), runner.eventsFor("--file=" + file)
                .stream().map(LedgerEvent::eventId).toList());
        assertEquals(List.of("E1"), runner.eventsFor("--file", file.toString())
                .stream().map(LedgerEvent::eventId).toList());
    }

    @Test
    void runnerRejectsMissingFilePath() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new ConsoleReplayRunner().eventsFor("--file"));

        assertTrue(exception.getMessage().contains("--file requires"));
    }
}
