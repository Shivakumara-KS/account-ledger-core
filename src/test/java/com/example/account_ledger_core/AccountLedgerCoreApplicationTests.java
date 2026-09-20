package com.example.account_ledger_core;

import com.example.account_ledger_core.application.*;
import com.example.account_ledger_core.domain.*;
import com.example.account_ledger_core.infrastructure.RequirementFixture;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AccountLedgerCoreApplicationTests {
    @Test
    void contextLoads() {
    }

    @Test
    void moneyUsesCurrencyPrecisionAndRejectsMixedCurrencies() {
        assertEquals(new BigDecimal("10.002"), Money.of(CurrencyCode.BHD, "10.0015").amount());
        assertThrows(IllegalArgumentException.class, () ->
                Money.of(CurrencyCode.AED, "1.00").add(Money.of(CurrencyCode.BHD, "1.000")));
    }

    @Test
    void replayProducesDeterministicEndToEndResult() {
        ReplayEngine engine = new ReplayEngine();
        ReplayResult result = engine.replay(RequirementFixture.events());
        assertEquals(Money.of(CurrencyCode.AED, "466.03"), result.finalBalances().get(new AccountId("ACC-001")));
        assertEquals(Money.of(CurrencyCode.BHD, "10.008"), result.finalBalances().get(new AccountId("ACC-002")));
        assertEquals(1, result.processingErrors().stream().filter(e -> e.code() == ErrorCode.SETTLEMENT_AUTH_NOT_FOUND).count());
        assertEquals(3, result.feeAssessments().size());
        assertEquals(3, result.feeReversals().size());
        assertEquals(AuthorizationState.APPROVED, result.dailyReports().get(SimulationDay.DAY2)
                .authorizations().stream().findFirst().orElseThrow().state());
        assertEquals(Money.of(CurrencyCode.AED, "250.00"),
                result.dailyReports().get(SimulationDay.DAY2).closingBalances().get(new AccountId("ACC-001")));
        assertEquals(Money.of(CurrencyCode.AED, "-230.00"),
                result.dailyReports().get(SimulationDay.DAY5).closingBalances().get(new AccountId("ACC-001")));
        assertEquals(AuthorizationState.SETTLED, result.dailyReports().get(SimulationDay.DAY4)
                .authorizations().stream().findFirst().orElseThrow().state());
        assertEquals(result, engine.replay(RequirementFixture.events()));
    }

    @Test
    void appendOnlyHistoryRetainsFeeAssessmentsAndReversals() {
        ReplayResult result = new ReplayEngine().replay(RequirementFixture.events());
        assertEquals(3, result.feeAssessments().size());
        assertEquals(3, result.feeReversals().size());
        assertTrue(result.feeAssessments().stream().allMatch(e -> e.entryType() == LedgerEntryType.OVERDRAFT_FEE));
        assertTrue(result.feeReversals().stream().allMatch(e -> e.reversalOfEntryId() != null));
        assertEquals(1, result.ledgerEntries().stream().filter(e -> e.entryId().equals("E7")
                && e.entryType() == LedgerEntryType.DEBIT).count());
        assertEquals("E7", result.ledgerEntries().stream()
                .filter(e -> e.entryType() == LedgerEntryType.REVERSAL_POSTING)
                .findFirst().orElseThrow().reversalOfEntryId());
    }

    @Test
    void installmentRemainderIsAllocatedWithoutMoneyCreation() {
        ReplayResult result = new ReplayEngine().replay(RequirementFixture.events());
        assertEquals(java.util.List.of(new BigDecimal("3.334"), new BigDecimal("3.333"), new BigDecimal("3.333")),
                result.ledgerEntries().stream().filter(e -> e.entryType() == LedgerEntryType.INSTALLMENT)
                        .map(e -> e.amount().amount()).toList());
    }

    @Test
    void duplicateIngestionIsRetainedButNeverPosted() {
        var events = new ArrayList<>(RequirementFixture.events());
        events.add(events.get(0));
        ReplayResult result = new ReplayEngine().replay(events);
        assertEquals(11, result.eventHistory().size());
        assertEquals(10, result.acceptedEvents().size());
        assertEquals(1, result.rejectedIngestionAttempts().size());
        assertEquals(ErrorCode.DUPLICATE_EVENT_ID, result.processingErrors().stream()
                .filter(e -> e.eventId().equals("E1")).findFirst().orElseThrow().code());
        assertEquals(Money.of(CurrencyCode.AED, "466.03"),
                result.finalBalances().get(new AccountId("ACC-001")));
    }

    @Test
    void reversalCannotTargetARejectedDebit() {
        var events = List.<LedgerEvent>of(
                new CreditEvent("E1", 1, new AccountId("ACC-001"), Money.of(CurrencyCode.AED, "100"),
                        SimulationDay.DAY1, SimulationDay.DAY1),
                new DebitEvent("E2", 99, new AccountId("ACC-001"), Money.of(CurrencyCode.AED, "50"),
                        SimulationDay.DAY1, SimulationDay.DAY1),
                new ReversalEvent("E3", 3, new AccountId("ACC-001"), "E2", CurrencyCode.AED,
                        SimulationDay.DAY1, SimulationDay.DAY1));

        ReplayResult result = new ReplayEngine().replay(events);

        assertEquals(ErrorCode.EVENT_NOT_FOUND, result.processingErrors().stream()
                .filter(error -> error.eventId().equals("E3")).findFirst().orElseThrow().code());
        assertTrue(result.ledgerEntries().stream()
                .noneMatch(entry -> entry.entryType() == LedgerEntryType.REVERSAL_POSTING));
    }

    @Test
    void settlementCannotExceedAuthorizationHold() {
        var events = List.<LedgerEvent>of(
                new CreditEvent("E1", 1, new AccountId("ACC-001"), Money.of(CurrencyCode.AED, "100"),
                        SimulationDay.DAY1, SimulationDay.DAY1),
                new AuthorizationEvent("E2", 2, new AccountId("ACC-001"), new AuthorizationId("AUTH-1"),
                        Money.of(CurrencyCode.AED, "50"), SimulationDay.DAY1, SimulationDay.DAY1),
                new SettlementEvent("E3", 3, new AccountId("ACC-001"), new AuthorizationId("AUTH-1"),
                        Money.of(CurrencyCode.AED, "60"), SimulationDay.DAY1, SimulationDay.DAY1));

        ReplayResult result = new ReplayEngine().replay(events);

        assertEquals(ErrorCode.SETTLEMENT_AMOUNT_INVALID, result.processingErrors().stream()
                .filter(error -> error.eventId().equals("E3")).findFirst().orElseThrow().code());
        assertTrue(result.ledgerEntries().stream()
                .noneMatch(entry -> entry.entryType() == LedgerEntryType.SETTLEMENT_DEBIT));
    }

    @Test
    void duplicateAuthorizationDoesNotReplaceTheOriginal() {
        var events = List.<LedgerEvent>of(
                new CreditEvent("E1", 1, new AccountId("ACC-001"), Money.of(CurrencyCode.AED, "100"),
                        SimulationDay.DAY1, SimulationDay.DAY1),
                new AuthorizationEvent("E2", 2, new AccountId("ACC-001"), new AuthorizationId("AUTH-1"),
                        Money.of(CurrencyCode.AED, "20"), SimulationDay.DAY1, SimulationDay.DAY1),
                new AuthorizationEvent("E3", 3, new AccountId("ACC-001"), new AuthorizationId("AUTH-1"),
                        Money.of(CurrencyCode.AED, "80"), SimulationDay.DAY1, SimulationDay.DAY1));

        ReplayResult result = new ReplayEngine().replay(events);

        assertEquals(ErrorCode.DUPLICATE_AUTHORIZATION_ID, result.processingErrors().stream()
                .filter(error -> error.eventId().equals("E3")).findFirst().orElseThrow().code());
        assertEquals(Money.of(CurrencyCode.AED, "20"), result.authorizations().getFirst().holdAmount());
    }
}
