package com.example.account_ledger_core.domain;

public enum SimulationDay {
    DAY1(1),
    DAY2(2),
    DAY3(3),
    DAY4(4),
    DAY5(5),
    DAY6(6);
    private final int number;

    SimulationDay(int number) {
        this.number = number;
    }

    public int number() {
        return number;
    }
}
