package com.jdom.util

import spock.lang.Specification
import spock.lang.Unroll

import static com.jdom.util.MathUtil.formatMoney
import static com.jdom.util.MathUtil.formatPercentage
import static com.jdom.util.MathUtil.percentChange

/**
 * Created by djohnson on 12/8/14.
 */
class MathUtilSpec extends Specification {

    @Unroll
    def 'should return correct values for percent change'() {

        expect:
        expectedValue == percentChange(numerator, denominator)

        where:
        numerator | denominator | expectedValue
        -11534L   | 20000L      | -15767L
        11534L    | 20000L      | -4233L
        20000L    | 11534L      | 7340L
        40000L    | 20000L      | 10000L
        60000L    | 20000L      | 20000L
    }

    @Unroll
    def 'should format percentages correctly'() {

        expect:
        expectedValue == formatPercentage(percentage)

        where:
        percentage | expectedValue
        -4233L     | '-42.33%'
        0L         | '0.00%'
        513L       | '5.13%'
        11534L     | '115.34%'
        20007L     | '200.07%'
    }

    @Unroll
    def 'should format money correctly'() {

        expect:
        expectedValue == formatMoney(value)

        where:
        value  | expectedValue
        -4233L | '-$42.33'
        0L     | '$0.00'
        513L   | '$5.13'
        11534L | '$115.34'
        20007L | '$200.07'
    }

}
