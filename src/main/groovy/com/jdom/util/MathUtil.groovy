package com.jdom.util

/**
 * Created by djohnson on 11/16/14.
 */
class MathUtil {

    private MathUtil() {
    }

    static String formatPositiveOrNegative(double value) {
        if (value < 0) {
            return Double.toString(value)
        } else {
            return "+" + Double.toString(value)
        }
    }

    static String formatMoney(long money) {
        int dollars = (money / 100)
        def cents = "${Math.abs(money) % 100}".padLeft(2, '0')

        return ((dollars < 0) ? '-$' : '$') + Math.abs(dollars) + "." + cents
    }

    static long toMoney(final String fromString) {
        def toParse = (fromString.startsWith('$')) ? fromString.substring(1) : fromString
        return new BigDecimal(toParse).multiply(new BigDecimal(100)).longValue()
    }

    static String formatPercentage(long percentage) {
        int wholeNumber = (percentage / 100)
        def remainder = "${Math.abs(percentage) % 100}".padLeft(2, '0')

        return wholeNumber + "." + remainder + '%'
    }

    static long toPercentage(final String fromString) {
        def toParse = (fromString.endsWith('%')) ? fromString.substring(0, fromString.length() - 1) : fromString
        return new BigDecimal(toParse).multiply(new BigDecimal(100)).longValue()
    }

    /**
     * Returns percent change as a long.  57.67% represented as 5767L
     * @param numerator
     * @param denominator
     * @return
     */
    static long percentChange(final long numerator, final long denominator) {
        long multiplier = 10000
        long percentage = (numerator * multiplier) / denominator

        return (percentage - 10000)
    }
}
