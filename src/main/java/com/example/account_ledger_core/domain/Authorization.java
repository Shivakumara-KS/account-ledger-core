package com.example.account_ledger_core.domain;
public record Authorization(
        AuthorizationId authorizationId,
        AccountId accountId,
        CurrencyCode currency,
        Money holdAmount,
        SimulationDay bookingDay,
        AuthorizationState state) {
    public Authorization withState(AuthorizationState next) {
        return new Authorization(authorizationId, accountId, currency, holdAmount, bookingDay, next);
    }
}
