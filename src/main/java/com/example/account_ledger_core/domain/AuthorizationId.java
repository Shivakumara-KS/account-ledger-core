package com.example.account_ledger_core.domain;
public record AuthorizationId(String value) {
    public AuthorizationId {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException("authorization id");
    }
    @Override
    public String toString() {
        return value;
    }
}
