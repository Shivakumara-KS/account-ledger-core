package com.example.account_ledger_core.domain;

public record AccountId(String value) {
    public AccountId {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException("account id");
    }

    @Override
    public String toString() {
        return value;
    }
}
