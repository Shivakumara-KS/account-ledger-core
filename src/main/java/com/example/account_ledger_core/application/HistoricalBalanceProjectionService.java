package com.example.account_ledger_core.application;

import com.example.account_ledger_core.domain.*;
import java.util.*;

public final class HistoricalBalanceProjectionService {
    public Money balance(AccountId account, SimulationDay valueDate, SimulationDay asOf,
                         List<LedgerEntry> entries, CurrencyCode currency) {
        Money result = Money.of(currency, "0");
        for (LedgerEntry entry : entries) {
            if (entry.accountId().equals(account)
                    && entry.bookingDay().number() <= asOf.number()
                    && entry.valueDate().number() <= valueDate.number()) {
                result = result.add(entry.signedAmount());
            }
        }
        return result;
    }

    public Map<SimulationDay, Money> finalBalances(AccountId account, CurrencyCode currency,
                                                    List<LedgerEntry> entries) {
        EnumMap<SimulationDay, Money> result = new EnumMap<>(SimulationDay.class);
        for (SimulationDay day : SimulationDay.values()) {
            result.put(day, balance(account, day, SimulationDay.DAY6, entries, currency));
        }
        return Collections.unmodifiableMap(result);
    }
}
