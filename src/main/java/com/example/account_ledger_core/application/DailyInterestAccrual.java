package com.example.account_ledger_core.application;

import com.example.account_ledger_core.domain.*;

public record DailyInterestAccrual(
        AccountId accountId,
        SimulationDay day,
        Money amount) {}
