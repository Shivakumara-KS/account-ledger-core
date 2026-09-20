package com.example.account_ledger_core.domain;
public record ProcessingError(
        String eventId,
        ErrorCode code,
        String message
) {}
