package com.jdom.util

/**
 * Created by djohnson on 12/20/14.
 */
class MACD {
    // Why does MACD require 34 days when it uses 26 in the calculation?
    static final int TOO_FEW_DAYS_FOR_WINDOW = 33

    long value
    long signal
    long histogram
}
