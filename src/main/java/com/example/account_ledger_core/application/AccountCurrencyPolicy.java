package com.example.account_ledger_core.application;

import com.example.account_ledger_core.domain.AccountId;
import com.example.account_ledger_core.domain.CurrencyCode;

import java.util.Map;

public final class AccountCurrencyPolicy {
    private static final Map<String, CurrencyCode> REQUIRED = Map.of(
            "ACC-001", CurrencyCode.AED,
            "ACC-002", CurrencyCode.BHD);

    public void validate(AccountId account, CurrencyCode currency) {
        CurrencyCode required = REQUIRED.get(account.value());
        if (required != null && required != currency) {
            throw new IllegalArgumentException("account " + account + " must use " + required);
        }
    }
}
