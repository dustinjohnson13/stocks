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
        int dollarsAsInt = (money / 100)
        def dollars = Integer.toString(dollarsAsInt).padLeft(2, '0')
        def cents = "${money % 100}".padLeft(2, '0')

        return "\$" + dollars + "." + cents
    }
}
