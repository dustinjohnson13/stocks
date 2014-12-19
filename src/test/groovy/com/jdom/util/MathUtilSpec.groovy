package com.jdom.util

import spock.lang.Specification
import spock.lang.Unroll

import static com.jdom.util.MathUtil.formatMoney
import static com.jdom.util.MathUtil.formatPercentage
import static com.jdom.util.MathUtil.percentChange
import static com.jdom.util.MathUtil.simpleMovingAverage

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

    @Unroll
    def 'should calculate simple moving average correctly'() {

        def array = JavaUtil.convertLongListToPrimitivateArray(values)
        def expected = JavaUtil.convertLongListToPrimitivateArray(expectedValue)
        def actual = simpleMovingAverage(array, period)

        expect:
        actual == expected

        where:
        values                            | expectedValue                   | period
        [2l, 4l, 8l, 16l, 32l]            | [0l, 0l, 0l, 0l, 12l]           | 5
        [2l, 4l, 8l, 16l, 32l, 64l]       | [0l, 0l, 0l, 0l, 12l, 25l]      | 5
        [2l, 4l, 8l, 16l, 32l, 64l, 128l] | [0l, 0l, 0l, 0l, 12l, 25l, 50l] | 5
    }

    @Unroll
    def 'should throw exception on calculating simple moving average with too few of values for the period'() {

        def array = JavaUtil.convertLongListToPrimitivateArray(values)

        when:
        "an array of ${array.length} items is passed as an argument with a period of ${period}"
        simpleMovingAverage(array, period)

        then:
        thrown IllegalArgumentException

        where:
        values            | period
        [2l]              | 5
        [2l, 4l]          | 5
        [2l, 4l, 8l]      | 5
        [2l, 4l, 8l, 16l] | 5
    }

}
