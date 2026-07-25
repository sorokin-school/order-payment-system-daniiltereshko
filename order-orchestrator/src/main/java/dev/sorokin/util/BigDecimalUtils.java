package dev.sorokin.util;

import java.math.BigDecimal;

public final class BigDecimalUtils {

    public static boolean isGreaterThan(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) return false;

        return a.compareTo(b) > 0;
    }

}
