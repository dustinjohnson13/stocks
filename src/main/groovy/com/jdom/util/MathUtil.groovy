package com.jdom.util

import com.tictactec.ta.lib.Core
import com.tictactec.ta.lib.MAType
import com.tictactec.ta.lib.MInteger
import com.tictactec.ta.lib.RetCode

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

    static long[] simpleMovingAverage(long[] values, int period) {
        return calculateMovingAverage(values, period) { core, startIdx, endIdx, doubleValues, period2, outBegIdx, outNBElement, output ->
            return core.sma(startIdx, endIdx, doubleValues, period2, outBegIdx, outNBElement, output)
        }
    }

    static long[] exponentialMovingAverage(final long[] values, final int period) {
        return calculateMovingAverage(values, period) { core, startIdx, endIdx, doubleValues, period2, outBegIdx, outNBElement, output ->
            return core.ema(startIdx, endIdx, doubleValues, period2, new MInteger(), new MInteger(), output)
        }
    }

    private static long[] calculateMovingAverage(final long[] values, final int period, Closure<RetCode> calculate) {
        if (values.length < period) {
            throw new IllegalArgumentException("At least ${period} values must be passed to calculate with the given period.")
        }

        double[] doubleValues = translateLongArrayToDouble(values)

        def core = new Core()
        double[] output = new double[values.length]

        RetCode retCode = calculate.call(core, 0, values.length - 1, doubleValues, period, new MInteger(), new MInteger(), output)

        if (retCode != RetCode.Success) {
            throw new IllegalStateException("Ta-Lib error: ${retCode}")
        }

        return translateTaLibOutputToExpectedOrder(output, period - 1)
    }

    /**
     * Ta-Lib puts all of the uncalculated/default values at the end of the array.  For instance, with a simple moving average
     * then the days before the period length (e.g. 5 day SMA, then the first 4 days which can't be calculated) are placed at the end
     * of the array.
     *
     * @param taLibOutput
     *  the raw ta lib output
     * @param expectedZeros
     *  the number of zeros expected to be at the end (4 for a 5 day SMA) containing zeros (the opt-in period for a moving average)
     * @return
     */
    private static long[] translateTaLibOutputToExpectedOrder(final double[] taLibOutput, final int expectedZeros) {
        long[] retVal = new long[taLibOutput.length]

        int j = 0
        for (; j < expectedZeros; j++) {
            retVal[j] = 0

            // Make sure the correct number of expected zeros actually exist
            def actualValueThatIsExpectedToBeZero = taLibOutput[taLibOutput.length - (j + 1)]
            assert actualValueThatIsExpectedToBeZero == 0
        }

        for (int i = 0; i < taLibOutput.length - expectedZeros; i++) {
            retVal[j++] = Math.round(taLibOutput[i])
        }
        return retVal
    }

    static MACD macd(final long[] values, int fastPeriod, int slowPeriod, int signalPeriod) {
        if (!(values.length > MACD.TOO_FEW_DAYS_FOR_WINDOW)) {
            throw new IllegalArgumentException("At least ${MACD.TOO_FEW_DAYS_FOR_WINDOW + 1} values must be passed to calculate.")
        }

        double[] doubleValues = translateLongArrayToDouble(values)

        def core = new Core()
        double[] outMACD = new double[values.length]
        double[] outMACDSignal = new double[values.length]
        double[] outMACDHist = new double[values.length]

        RetCode retCode = core.macd(0, values.length - 1, doubleValues, fastPeriod, slowPeriod, signalPeriod,
              new MInteger(), new MInteger(), outMACD, outMACDSignal, outMACDHist)

        if (retCode != RetCode.Success) {
            throw new IllegalStateException("Ta-Lib error: ${retCode}")
        }

        long[] macd = translateTaLibOutputToExpectedOrder(outMACD, MACD.TOO_FEW_DAYS_FOR_WINDOW)
        long[] macdSignal = translateTaLibOutputToExpectedOrder(outMACDSignal, MACD.TOO_FEW_DAYS_FOR_WINDOW)
        long[] macdHist = translateTaLibOutputToExpectedOrder(outMACDHist, MACD.TOO_FEW_DAYS_FOR_WINDOW)

        return new MACD(value: macd[macd.length - 1], signal: macdSignal[macdSignal.length - 1],
              histogram: macdHist[macdHist.length - 1])
    }

    static Stochastic fastStochastic(final long[] highValues, final long[] lowValues, final long[] closeValues,
                                     final int kPeriod, final int dPeriod) {
        return stochastic(highValues, lowValues, closeValues,
              kPeriod, dPeriod, Stochastic.TOO_FEW_DAYS_FOR_WINDOW_FOR_FAST) { core, begIdx, endIdx, inputHighValues,
                                                                               inputLowValues, inputCloseValues,
                                                                               kPeriod2, dPeriod2, maType, idxInteger,
                                                                               elementInteger, outK, outD ->
            return core.stochF(begIdx, endIdx, inputHighValues, inputLowValues, inputCloseValues,
                  kPeriod2, dPeriod2, maType, idxInteger, elementInteger, outK, outD)
        }
    }

    static double[] translateLongArrayToDouble(final long[] values) {
        double[] doubleValues = new double[values.length]
        for (int i = 0; i < values.length; i++) {
            doubleValues[i] = values[i]
        }
        return doubleValues
    }

    static Stochastic slowStochastic(
          final long[] highValues, final long[] lowValues, final long[] closeValues, final int kPeriod,
          final int dPeriod, final int slowKPeriod) {
        return stochastic(highValues, lowValues, closeValues,
              kPeriod, dPeriod, Stochastic.TOO_FEW_DAYS_FOR_WINDOW_FOR_SLOW) { core, begIdx, endIdx, inputHighValues, inputLowValues, inputCloseValues,
                                                                               kPeriod2, dPeriod2, maType, idxInteger, elementInteger, outK, outD ->
            final MAType slowKMAType = MAType.Sma
            return core.stoch(begIdx, endIdx, inputHighValues, inputLowValues, inputCloseValues,
                  kPeriod2, slowKPeriod, slowKMAType, dPeriod2, maType, idxInteger, elementInteger, outK, outD)
        }
    }

    private static Stochastic stochastic(
          final long[] highValues,
          final long[] lowValues,
          final long[] closeValues,
          final int kPeriod,
          final int dPeriod,
          final int tooFewDays,
          Closure<RetCode> stochasticFunction) {
        if (!(highValues.length > tooFewDays)) {
            throw new IllegalArgumentException("At least ${tooFewDays + 1} values must be passed to calculate.")
        }

        def core = new Core()
        double[] outK = new double[closeValues.length]
        double[] outD = new double[closeValues.length]

        double[] inputHighValues = translateLongArrayToDouble(highValues)
        double[] inputLowValues = translateLongArrayToDouble(lowValues)
        double[] inputCloseValues = translateLongArrayToDouble(closeValues)

        RetCode retCode = stochasticFunction.call(core, 0, closeValues.length - 1, inputHighValues, inputLowValues, inputCloseValues,
              kPeriod, dPeriod, MAType.Sma, new MInteger(), new MInteger(), outK, outD)

        if (retCode != RetCode.Success) {
            throw new IllegalStateException("Ta-Lib error: ${retCode}")
        }

        long[] kValues = translateTaLibOutputToExpectedOrder(outK, tooFewDays)
        long[] dValues = translateTaLibOutputToExpectedOrder(outD, tooFewDays)

        return new Stochastic(k: kValues[kValues.length - 1], d: dValues[dValues.length - 1])
    }

    static final int relativeStrengthIndex(final long[] closeValues, final int period) {

        final double[] doubleValues = translateLongArrayToDouble(closeValues)
        final double[] output = new double[closeValues.length]
        def core = new Core()

        RetCode retCode = core.rsi(0, closeValues.length - 1, doubleValues, period,
              new MInteger(), new MInteger(), output)

        if (retCode != RetCode.Success) {
            throw new IllegalStateException("Ta-Lib error: ${retCode}")
        }

        long[] translatedOutput = translateTaLibOutputToExpectedOrder(output, period)

        return translatedOutput[translatedOutput.length - 1]
    }

    static final int williamsR(
          final long[] highValues, final long[] lowValues, final long[] closeValues, final int period) {

        final double[] doubleClose = translateLongArrayToDouble(closeValues)
        final double[] doubleHigh = translateLongArrayToDouble(highValues)
        final double[] doubleLow = translateLongArrayToDouble(lowValues)
        final double[] output = new double[closeValues.length]
        def core = new Core()

        RetCode retCode = core.willR(0, closeValues.length - 1, doubleHigh, doubleLow, doubleClose, period,
              new MInteger(), new MInteger(), output)

        if (retCode != RetCode.Success) {
            throw new IllegalStateException("Ta-Lib error: ${retCode}")
        }

        long[] translatedOutput = translateTaLibOutputToExpectedOrder(output, period - 1)

        return translatedOutput[translatedOutput.length - 1]
    }

    static final BollingerBands bollingerBands(final long[] closeValues, final int period) {

        final double[] doubleClose = translateLongArrayToDouble(closeValues)
        final double[] highBandOutput = new double[closeValues.length]
        final double[] middleBandOutput = new double[closeValues.length]
        final double[] lowerBandOutput = new double[closeValues.length]

        def core = new Core()

        double deviationsUp = 2
        double deviationsDown = deviationsUp
        RetCode retCode = core.bbands(0, closeValues.length - 1, doubleClose, period, deviationsUp, deviationsDown,
              MAType.Sma, new MInteger(), new MInteger(), highBandOutput, middleBandOutput, lowerBandOutput)

        if (retCode != RetCode.Success) {
            throw new IllegalStateException("Ta-Lib error: ${retCode}")
        }

        long[] translatedHighOutput = translateTaLibOutputToExpectedOrder(highBandOutput, period - 1)
        long[] translatedMiddleOutput = translateTaLibOutputToExpectedOrder(middleBandOutput, period - 1)
        long[] translatedLowerOutput = translateTaLibOutputToExpectedOrder(lowerBandOutput, period - 1)

        return new BollingerBands(upper: translatedHighOutput[translatedHighOutput.length - 1],
              middle: translatedMiddleOutput[translatedMiddleOutput.length - 1],
              lower: translatedLowerOutput[translatedLowerOutput.length - 1])
    }

    static int commodityChannelIndex(
          final long[] highValues, final long[] lowValues, final long[] closeValues, final int period) {

        final double[] doubleClose = translateLongArrayToDouble(closeValues)
        final double[] doubleHigh = translateLongArrayToDouble(highValues)
        final double[] doubleLow = translateLongArrayToDouble(lowValues)
        final double[] output = new double[closeValues.length]

        def core = new Core()

        RetCode retCode = core.cci(0, doubleClose.length - 1, doubleHigh, doubleLow, doubleClose, period,
              new MInteger(), new MInteger(), output)

        if (retCode != RetCode.Success) {
            throw new IllegalStateException("Ta-Lib error: ${retCode}")
        }

        long[] translatedOutput = translateTaLibOutputToExpectedOrder(output, period - 1)

        return translatedOutput[translatedOutput.length - 1]
    }
}
