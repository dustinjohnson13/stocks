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
}
