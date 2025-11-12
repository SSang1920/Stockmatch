package com.stockmatch.portfolio.mapper;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class ValuationMapper {

    private static final int SCALE_PRICE = 4;
    private static final int SCALE_MONEY = 2;
    private static final int SCALE_RATE = 6;

    private ValuationMapper() {}

    public static BigDecimal mulMoney(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) return BigDecimal.ZERO;
        return a.multiply(b).setScale(SCALE_MONEY, RoundingMode.HALF_UP);
    }

    public static BigDecimal subMoney(BigDecimal a, BigDecimal b) {
        if (a == null) return a = BigDecimal.ZERO;
        if (b == null) return b = BigDecimal.ZERO;
        return a.subtract(b).setScale(SCALE_MONEY, RoundingMode.HALF_UP);
    }

    public static BigDecimal safeDiv(BigDecimal a, BigDecimal b, int scale) {
        if (a == null || b == null || b.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(scale, RoundingMode.HALF_UP);
        }
        return a.divide(b, scale, RoundingMode.HALF_UP);
    }

    public static BigDecimal toRate(BigDecimal value, BigDecimal base) {
        var ratio = safeDiv(value, base, SCALE_RATE);
        return ratio.subtract(BigDecimal.ONE).setScale(SCALE_RATE, RoundingMode.HALF_UP);
    }

    public static int priceScale() {
        return SCALE_PRICE;
    }
}
