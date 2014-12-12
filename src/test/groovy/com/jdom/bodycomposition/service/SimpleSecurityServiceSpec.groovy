package com.jdom.bodycomposition.service

import com.jdom.bodycomposition.domain.DailySecurityData
import com.jdom.bodycomposition.domain.DailySecurityMetrics
import com.jdom.bodycomposition.domain.Stock
import com.jdom.bodycomposition.domain.algorithm.BuyTransaction
import com.jdom.bodycomposition.domain.algorithm.PositionValue
import com.jdom.bodycomposition.domain.market.MarketReplay
import com.jdom.bodycomposition.domain.algorithm.Portfolio
import com.jdom.bodycomposition.domain.algorithm.PortfolioValue
import com.jdom.bodycomposition.domain.algorithm.Position
import com.jdom.bodycomposition.domain.algorithm.SellTransaction
import com.jdom.bodycomposition.domain.algorithm.impl.TestMsftAlgorithm
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.transaction.TransactionConfiguration
import spock.lang.Specification
import spock.lang.Unroll

import javax.transaction.Transactional

import static com.jdom.util.MathUtil.toMoney
import static com.jdom.util.MathUtil.toPercentage
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

    def 'should be able to profile algorithms'() {

        def MSFT = new Stock(symbol: 'MSFT')

        def expectedTransactions = [
              new BuyTransaction(MSFT, dateFromDashString('2003-11-28'), 2, toMoney('$25.50'), toMoney('$4.95')),
              new SellTransaction(MSFT, dateFromDashString('2004-03-18'), 2, toMoney('$24.89'), toMoney('$4.95')),
              new BuyTransaction(MSFT, dateFromDashString('2005-05-18'), 2, toMoney('$25.50'), toMoney('$4.95')),
              new BuyTransaction(MSFT, dateFromDashString('2010-07-14'), 2, toMoney('$25.50'), toMoney('$4.95')),
              new BuyTransaction(MSFT, dateFromDashString('2010-07-15'), 2, toMoney('$25.50'), toMoney('$4.95')),
              new SellTransaction(MSFT, dateFromDashString('2010-07-16'), 2, toMoney('$24.89'), toMoney('$4.95'))
        ]

        given: 'a portfolio of $200 and a commission of $4.95'
        Portfolio initialPortfolio = new Portfolio(toMoney('$200'), toMoney('$4.95'))

        and: 'purchasing/selling MSFT stock between 11/27/2003 and 07/16/2010'
        MarketReplay scenario = new MarketReplay(initialPortfolio: initialPortfolio,
              algorithm: new TestMsftAlgorithm(),
              startDate: dateFromDashString('2003-11-26'),
              endDate: dateFromDashString('2010-07-16'))

        when: 'the algorithm is profiled'
        MarketReplay result = service.profileAlgorithm(scenario)
        PortfolioValue resultPortfolio = result.resultPortfolio

        then: 'the result contains the correct amount of cash'
        resultPortfolio.cash == toMoney('$65.86')

        def positions = resultPortfolio.positions
        and: 'the result contains the correct number of positions'
        positions.size() == 1

        def position = positions.iterator().next()
        and: 'the position has the correct attributes'
        position.security == MSFT
        position.shares == 2

        def transactions = result.transactions
        and: 'the correct number of transactions exist'
        transactions.size() == 6

        and: 'the transactions have the correct attributes'
        transactions == expectedTransactions

        and: 'the portfolio value change is correct'
        result.valueChangePercent == toPercentage('-42.18%')
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
        Portfolio portfolio = new Portfolio(cash, toMoney('$4.98'), positions)

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

        where:
        high     | low      | expected52WkHighDate | expected52WkHighValue | expected52WkLowDate | expected52WkLowValue
        '$48.97' | '$48.38' | '2014-11-14'         | '$50.05'              | '2014-01-14'        | '$34.63' // Today neither 52 week high or low
        '$50.06' | '$48.38' | '2014-12-08'         | '$50.06'              | '2014-01-14'        | '$34.63' // Today 52 week high
        '$48.97' | '$34.62' | '2014-11-14'         | '$50.05'              | '2014-12-08'        | '$34.62' // Today 52 week low
        '$50.05' | '$48.38' | '2014-12-08'         | '$50.05'              | '2014-01-14'        | '$34.63' // Today matches 52 week high
        '$48.97' | '$34.63' | '2014-11-14'         | '$50.05'              | '2014-12-08'        | '$34.63' // Today matches 52 week low
    }

    def 'should return portfolio value checkpoints'() {
        def msft = service.findSecurityBySymbol('MSFT')

        def day1 = dateFromDashString('2010-07-14')
        def day2 = dateFromDashString('2010-07-15')
        def day3 = dateFromDashString('2010-07-16')

        def day1Portfolio = new Portfolio(toMoney('$200'), toMoney('$5'))
        def day2Portfolio = new Portfolio(toMoney('$200'), toMoney('$5'), [new Position(msft, 2)] as Set)
        def day3Portfolio = new Portfolio(toMoney('$180'), toMoney('$5'), [new Position(msft, 3)] as Set)

        MarketReplay scenario = new MarketReplay(algorithm: new TestMsftAlgorithm(),
              startDate: day1,
              endDate: day3,
              portfolioByDate: [
                    (day1): day1Portfolio,
                    (day2): day2Portfolio,
                    (day3): day3Portfolio
              ]
        )

        when: 'the portfolio checkpoints are calculated'
        def portfolioValueCheckpoints = service.portfolioValueCheckpoints(scenario)

        then: 'the returned portfolio values are correct'
        portfolioValueCheckpoints == [
              new PortfolioValue(day1Portfolio, day1, [] as Set),
              new PortfolioValue(day2Portfolio, day2, [new PositionValue(new Position(msft, 2), day2, toMoney('$25.51'))] as Set),
              new PortfolioValue(day3Portfolio, day3, [new PositionValue(new Position(msft, 3), day3, toMoney('$24.89'))] as Set)
        ]

        and: 'the values are correct'
        portfolioValueCheckpoints[0].marketValue() == toMoney('$200')
        portfolioValueCheckpoints[1].marketValue() == toMoney('$251.02')
        portfolioValueCheckpoints[2].marketValue() == toMoney('$254.67')
    }
}
