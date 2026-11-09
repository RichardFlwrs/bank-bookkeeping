package com.orquidea.bank.card.application;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class PurchaseFee {

    private static final BigDecimal FEE_THRESHOLD = new BigDecimal("40.00");
    private static final BigDecimal FEE = new BigDecimal("1.50");
    private static final BigDecimal ZERO = new BigDecimal("0.00");

    private PurchaseFee() {
    }

    public static BigDecimal forAmount(BigDecimal amount) {
        if (amount == null) {
            return ZERO;
        }
        BigDecimal normalized = amount.setScale(2, RoundingMode.HALF_UP);
        if (normalized.compareTo(FEE_THRESHOLD) >= 0) {
            return FEE;
        }
        return ZERO;
    }
}
