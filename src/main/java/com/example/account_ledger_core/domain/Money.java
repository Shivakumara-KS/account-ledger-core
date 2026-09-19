package com.example.account_ledger_core.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Money(CurrencyCode currency, BigDecimal amount) {
    public Money {
        Objects.requireNonNull(currency, "currency");
        Objects.requireNonNull(amount, "amount");
        amount = amount.setScale(currency.scale(), RoundingMode.HALF_UP);
    }

    public static Money of(CurrencyCode c, String value) {
        return new Money(c, new BigDecimal(value));
    }

    public static Money of(CurrencyCode c, BigDecimal value) {
        return new Money(c, value);
    }

    public Money add(Money other) {
        check(other);
        return new Money(currency, amount.add(other.amount));
    }

    public Money subtract(Money other) {
        check(other);
        return new Money(currency, amount.subtract(other.amount));
    }

    public Money negate() {
        return new Money(currency, amount.negate());
    }

    public Money multiply(BigDecimal factor, int scale) {
        return new Money(currency, amount.multiply(factor).setScale(scale, RoundingMode.HALF_UP));
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    private void check(Money other) {
        Objects.requireNonNull(other, "other");
        if (currency != other.currency) throw new IllegalArgumentException("Currency mismatch");
    }
}
