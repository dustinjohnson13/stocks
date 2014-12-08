package com.jdom.bodycomposition.service
import com.jdom.bodycomposition.domain.BaseSecurity
import com.jdom.bodycomposition.domain.Stock
import com.jdom.bodycomposition.domain.algorithm.Algorithm
import com.jdom.bodycomposition.domain.algorithm.AlgorithmScenario
import com.jdom.bodycomposition.domain.algorithm.BuyTransaction
import com.jdom.bodycomposition.domain.algorithm.Portfolio
import com.jdom.bodycomposition.domain.algorithm.PortfolioValue
import com.jdom.bodycomposition.domain.algorithm.Position
import com.jdom.bodycomposition.domain.algorithm.SellTransaction
import com.jdom.bodycomposition.domain.algorithm.TestMsftAlgorithm
import org.springframework.beans.factory.annotation.Autowired
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
@ContextConfiguration(classes = [StocksContext.class])
@Transactional
@TransactionConfiguration(defaultRollback = true)
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
              new BuyTransaction(MSFT, dateFromDashString('2003-11-28'), 2, toMoney('$25.50'), toMoney('$5')),
              new SellTransaction(MSFT, dateFromDashString('2004-03-18'), 2, toMoney('$24.89'), toMoney('$5')),
              new BuyTransaction(MSFT, dateFromDashString('2005-05-18'), 2, toMoney('$25.50'), toMoney('$5')),
              new BuyTransaction(MSFT, dateFromDashString('2010-07-14'), 2, toMoney('$25.50'), toMoney('$5')),
              new BuyTransaction(MSFT, dateFromDashString('2010-07-15'), 2, toMoney('$25.50'), toMoney('$5')),
              new SellTransaction(MSFT, dateFromDashString('2010-07-16'), 2, toMoney('$24.89'), toMoney('$5'))
        ]

        given: 'a portfolio of $200 and a commission of $5'
        Portfolio initialPortfolio = new Portfolio(toMoney('$200'), toMoney('$5'))

        and: 'purchasing/selling MSFT stock between 11/27/2003 and 07/16/2010'
        AlgorithmScenario scenario = new AlgorithmScenario(initialPortfolio: initialPortfolio,
              algorithm: new TestMsftAlgorithm(),
              startDate: dateFromDashString('2003-11-27'),
              endDate: dateFromDashString('2010-07-16'))

        when: 'the algorithm is profiled'
        AlgorithmScenario result = service.profileAlgorithm(scenario)
        PortfolioValue resultPortfolio = result.resultPortfolio

        then: 'the result contains the correct amount of cash'
        resultPortfolio.cash == toMoney('$65.56')

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
        result.valueChangePercent == toPercentage('-42.33%')
    }

    def 'should evaluate each included security for a day before continuing to the next day'() {

        Portfolio initialPortfolio = new Portfolio(toMoney('$200'), toMoney('$5'))
        def july15th2014 = dateFromDashString('2014-07-15')
        def july16th2014 = dateFromDashString('2014-07-16')

        given: 'an algorithm including both MSFT, EBF, and GOOG securities'
        Algorithm algorithm = Mock()
        _ * algorithm.includeSecurity(_ as BaseSecurity) >> { BaseSecurity security -> return ['MSFT', 'EBF', 'GOOG'].contains(security.symbol) }

        when: 'the algorithm is profiled over two days'
        AlgorithmScenario scenario = new AlgorithmScenario(initialPortfolio: initialPortfolio,
              algorithm: algorithm,
              startDate: july15th2014,
              endDate: july16th2014)

        service.profileAlgorithm(scenario)

        then: 'each security is evaluated for the first day'
        1 * algorithm.actionsForDay(_ as Portfolio, { it.date == july15th2014 && it.security.symbol == 'EBF'} )
        1 * algorithm.actionsForDay(_ as Portfolio, { it.date == july15th2014 && it.security.symbol == 'GOOG'} )
        1 * algorithm.actionsForDay(_ as Portfolio, { it.date == july15th2014 && it.security.symbol == 'MSFT'} )

        then: 'each security is evaluated for the next day'
        1 * algorithm.actionsForDay(_ as Portfolio, { it.date == july16th2014 && it.security.symbol == 'EBF'} )
        1 * algorithm.actionsForDay(_ as Portfolio, { it.date == july16th2014 && it.security.symbol == 'GOOG'} )
        1 * algorithm.actionsForDay(_ as Portfolio, { it.date == july16th2014 && it.security.symbol == 'MSFT'} )
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
}
