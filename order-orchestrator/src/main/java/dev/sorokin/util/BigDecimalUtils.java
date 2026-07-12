package dev.sorokin.util;

import java.math.BigDecimal;

public final class BigDecimalUtils {

    public static boolean isGreaterThan(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) > 0;
    }

    public static boolean isLessThan(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) < 0;
    }

    public static boolean isEqual(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) == 0;
    }

    public static boolean isGreaterThanOrEqual(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) >= 0;
    }

    public static boolean isLessThanOrEqual(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) <= 0;
    }

    public static boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    public static boolean isZero(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) == 0;
    }
}
