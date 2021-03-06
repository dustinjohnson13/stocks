package com.jdom.bodycomposition.service

import com.jdom.bodycomposition.domain.DailySecurityData
import com.jdom.bodycomposition.domain.DailySecurityMetrics
import com.jdom.bodycomposition.domain.algorithm.Portfolio
import com.jdom.bodycomposition.domain.algorithm.Position
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.transaction.TransactionConfiguration
import spock.lang.Specification
import spock.lang.Unroll

import javax.transaction.Transactional

import static com.jdom.util.MathUtil.toMoney
import static com.jdom.util.TimeUtil.dateFromDashString

/**
 * Created by djohnson on 11/15/14.
 */
@ActiveProfiles(SpringProfiles.TEST)
@ContextConfiguration(classes = [StocksServiceContext.class])
@Transactional
@TransactionConfiguration(defaultRollback = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SimpleSecurityServiceSpec extends Specification {

    @Autowired
    SecurityService service

    def 'should return all stock tickers'() {

        when: 'all stocks are retrieved'
        def results = service.stocks
        def actualSize = results.size()

        then: 'the correct number of results were returned'
        actualSize == 6
    }

    @Unroll
    def 'should be able to get the value of a portfolio for a specific day'() {

        def securities = service.getStocks()

        given: 'a portfolio with two securities'
        def positions = new HashSet<Position>([
              Position.newPosition(securities.find { it.symbol == 'MSFT' }, 2),
              Position.newPosition(securities.find { it.symbol == 'EBF' }, 17)
        ])

        and: 'an amount of cash'
        def cash = toMoney('$457.33')
        Portfolio portfolio = Portfolio.newPortfolio(cash, positions)

        when: 'the service is queried for portfolio value on a specific day'
        long portfolioValue = service.portfolioValue(portfolio, date)?.marketValue()

        then: 'the portfolio value is correct'
        portfolioValue == expectedValue

        where:
        date                             | expectedValue
        dateFromDashString('2010-03-17') | toMoney('$798.45') // EBF $16.58, MSFT $29.63 -> (16.58 * 17) + (29.63 * 2) + 457.33
        dateFromDashString('2014-12-05') | toMoney('$787.75') // EBF $13.74, MSFT $48.42 -> (13.74 * 17) + (48.42 * 2) + 457.33
        dateFromDashString('2014-12-06') | toMoney('$787.75') // Saturday, use previous day's value
        dateFromDashString('1970-01-01') | toMoney('$457.33') // No data present, doesn't include position, only cash
    }

    @Unroll
    def 'should insert 52 week highs and lows when daily security data is inserted, 52WkHigh #expected52WkHighDate, 52WkLow #expected52WkLowDate'() {
        def msft = service.findSecurityBySymbol('MSFT')

        given: 'a new daily security data is persisted'
        DailySecurityData dailyData = new DailySecurityData(security: msft, date: dateFromDashString('2014-12-08'),
              open: toMoney('$48.82'), close: toMoney('$48.42'), high: toMoney(high), low: toMoney(low),
              volume: 26080000, adjustedClose: toMoney('$48.42'))

        when: 'new security daily data is inserted'
        service.save(dailyData)

        then: 'a new daily metrics entry was inserted'
        DailySecurityMetrics metrics = service.findDailySecurityMetricsBySecurityAndDate(msft, dailyData.date)
        metrics.id != null
        metrics.date == dailyData.date

        and: 'the 52 week highs and lows have the correct values'
        metrics.fiftyTwoWeekHigh != null
        metrics.fiftyTwoWeekHigh.date == dateFromDashString(expected52WkHighDate)
        metrics.fiftyTwoWeekHigh.high == toMoney(expected52WkHighValue)
        metrics.fiftyTwoWeekLow != null
        metrics.fiftyTwoWeekLow.date == dateFromDashString(expected52WkLowDate)
        metrics.fiftyTwoWeekLow.low == toMoney(expected52WkLowValue)

        where:
        high     | low      | expected52WkHighDate | expected52WkHighValue | expected52WkLowDate | expected52WkLowValue
        '$48.97' | '$48.38' | '2014-11-14'         | '$50.05'              | '2014-01-14'        | '$34.63' // Today neither 52 week high or low
        '$50.06' | '$48.38' | '2014-12-08'         | '$50.06'              | '2014-01-14'        | '$34.63' // Today 52 week high
        '$48.97' | '$34.62' | '2014-11-14'         | '$50.05'              | '2014-12-08'        | '$34.62' // Today 52 week low
        '$50.05' | '$48.38' | '2014-12-08'         | '$50.05'              | '2014-01-14'        | '$34.63' // Today matches 52 week high
        '$48.97' | '$34.63' | '2014-11-14'         | '$50.05'              | '2014-12-08'        | '$34.63' // Today matches 52 week low
    }

    def 'should insert technical analysis data when daily security data is inserted'() {
        def msft = service.findSecurityBySymbol('MSFT')

        given: 'a new daily security data is persisted'
        DailySecurityData dailyData = new DailySecurityData(security: msft, date: dateFromDashString('2014-12-08'),
              open: toMoney('$48.26'), close: toMoney('$47.70'), high: toMoney('$48.35'), low: toMoney('$47.44'),
              volume: 26663107, adjustedClose: toMoney('$48.42'))

        when: 'new security daily data is inserted'
        service.save(dailyData)

        then: 'a new daily metrics entry was inserted'
        DailySecurityMetrics metrics = service.findDailySecurityMetricsBySecurityAndDate(msft, dailyData.date)
        metrics.id != null
        metrics.date == dailyData.date

        and: 'the simple moving averages have the correct values'
        metrics.fiveDaySimpleMovingAverage == toMoney('$48.30')
        metrics.tenDaySimpleMovingAverage == toMoney('$48.07')
        metrics.twentyDaySimpleMovingAverage == toMoney('$48.48')
        metrics.fiftyDaySimpleMovingAverage == toMoney('$46.84')
        metrics.hundredDaySimpleMovingAverage == toMoney('$45.96')
        metrics.twoHundredDaySimpleMovingAverage == toMoney('$43.12')
        
        and: 'the exponential moving averages have the correct values'
        metrics.fiveDayExponentialMovingAverage == toMoney('$48.17')
        metrics.tenDayExponentialMovingAverage == toMoney('$48.21')
        metrics.twentyDayExponentialMovingAverage == toMoney('$48.07')
        metrics.fiftyDayExponentialMovingAverage == toMoney('$47.21')
        metrics.hundredDayExponentialMovingAverage == toMoney('$45.85')
        metrics.twoHundredDayExponentialMovingAverage == toMoney('$43.12')

        and: 'the MACD has the correct values'
        metrics.macd == 30l
        metrics.macdSignal == 43l

        and: 'the fast stochastic oscillator values are correct'
        metrics.fastStochasticOscillatorK == 20
        metrics.fastStochasticOscillatorD == 41

        and: 'the slow stochastic oscillator values are correct'
        metrics.slowStochasticOscillatorK == 41
        metrics.slowStochasticOscillatorD == 43

        and: 'the relative strength index is correct'
        metrics.relativeStrengthIndex == 45

        and: 'the williams %R is correct'
        metrics.williamsR == -80

        and: 'the bollinger band values are correct'
        metrics.bollingerBandsLower == 4721
        metrics.bollingerBandsUpper == 4974

        and: 'the commodity channel index is correct'
        metrics.commodityChannelIndex == -92
    }
}
