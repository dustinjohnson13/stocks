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
              new Position(securities.find { it.symbol == 'MSFT' }, 2),
              new Position(securities.find { it.symbol == 'EBF' }, 17)
        ])

        and: 'an amount of cash'
        def cash = toMoney('$457.33')
        Portfolio portfolio = new Portfolio(cash, positions)

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
    def 'should insert daily metrics when daily security data is inserted, 52WkHigh #expected52WkHighDate, 52WkLow #expected52WkLowDate'() {
        def msft = service.findSecurityBySymbol('MSFT')

        given: 'a new daily security data is persisted'
        DailySecurityData dailyData = new DailySecurityData(security: msft, date: dateFromDashString('2014-12-08'),
              open: toMoney('$48.82'), close: toMoney('$48.42'), high: toMoney(high), low: toMoney(low),
              volume: 26080000, adjustedClose: toMoney('$48.42'))

        when: 'new security daily data is inserted'
        service.save(dailyData)

        then: 'a new daily metrics entry was inserted'
        DailySecurityMetrics metrics = service.findDailySecurityMetricsBySecurityAndDate(msft, dailyData.date)

        and: 'the daily metrics entry has the correct values'
        metrics.id != null
        metrics.fiftyTwoWeekHigh != null
        metrics.fiftyTwoWeekHigh.date == dateFromDashString(expected52WkHighDate)
        metrics.fiftyTwoWeekHigh.high == toMoney(expected52WkHighValue)
        metrics.fiftyTwoWeekLow != null
        metrics.fiftyTwoWeekLow.date == dateFromDashString(expected52WkLowDate)
        metrics.fiftyTwoWeekLow.low == toMoney(expected52WkLowValue)
        metrics.date == dailyData.date
        metrics.fiveDayMovingAverage == toMoney(fiveDayMovingAverage)
        metrics.tenDayMovingAverage == toMoney(tenDayMovingAverage)
        metrics.twentyDayMovingAverage == toMoney(twentyDayMovingAverage)
        metrics.fiftyDayMovingAverage == toMoney(fiftyDayMovingAverage)
        metrics.hundredDayMovingAverage == toMoney(hundredDayMovingAverage)
        metrics.twoHundredDayMovingAverage == toMoney(twoHundredDayMovingAverage)

        where:
        high     | low      | expected52WkHighDate | expected52WkHighValue | expected52WkLowDate | expected52WkLowValue | fiveDayMovingAverage | tenDayMovingAverage | twentyDayMovingAverage | fiftyDayMovingAverage | hundredDayMovingAverage | twoHundredDayMovingAverage
        '$48.97' | '$48.38' | '2014-11-14'         | '$50.05'              | '2014-01-14'        | '$34.63'             | '$48.44'             | '$48.15'            | '$48.51'               | '$46.86'              | '$45.97'                | '$43.12'// Today neither 52 week high or low
        '$50.06' | '$48.38' | '2014-12-08'         | '$50.06'              | '2014-01-14'        | '$34.63'             | '$48.44'             | '$48.15'            | '$48.51'               | '$46.86'              | '$45.97'                | '$43.12'// Today 52 week high
        '$48.97' | '$34.62' | '2014-11-14'         | '$50.05'              | '2014-12-08'        | '$34.62'             | '$48.44'             | '$48.15'            | '$48.51'               | '$46.86'              | '$45.97'                | '$43.12'// Today 52 week low
        '$50.05' | '$48.38' | '2014-12-08'         | '$50.05'              | '2014-01-14'        | '$34.63'             | '$48.44'             | '$48.15'            | '$48.51'               | '$46.86'              | '$45.97'                | '$43.12'// Today matches 52 week high
        '$48.97' | '$34.63' | '2014-11-14'         | '$50.05'              | '2014-12-08'        | '$34.63'             | '$48.44'             | '$48.15'            | '$48.51'               | '$46.86'              | '$45.97'                | '$43.12'// Today matches 52 week low
    }
}
