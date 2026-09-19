package com.example.account_ledger_core.domain;

import java.math.BigDecimal;

public enum CurrencyCode {
    AED(2), BHD(3);

    private final int scale;
    CurrencyCode(int scale) { this.scale = scale; }
    public int scale() { return scale; }
    public BigDecimal zero() { return BigDecimal.ZERO.setScale(scale); }
}
