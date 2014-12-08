package com.jdom.bodycomposition.service

import com.jdom.bodycomposition.domain.algorithm.PortfolioValue
import com.jdom.bodycomposition.domain.Stock
import com.jdom.bodycomposition.domain.algorithm.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.transaction.TransactionConfiguration
import spock.lang.Specification

import javax.transaction.Transactional

import static com.jdom.util.MathUtil.toMoney
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
    }

    def 'should be able to get the value of a portfolio for a specific day'() {

        def securities = service.getStocks()

        given: 'a portfolio with two securities'
        def positions = new HashSet<Position>([
              new Position(securities.find{ it.symbol == 'MSFT'}, 2),
              new Position(securities.find{ it.symbol == 'EBF'}, 17)
        ])

        and: 'an amount of cash'
        def cash = toMoney('$457.33')
        Portfolio portfolio = new Portfolio(cash, toMoney('$4.98'), positions)

        when: 'the service is queried for portfolio value on a specific day'
        PortfolioValue march172010 = service.portfolioValue(portfolio, dateFromDashString('2010-03-17'))
        PortfolioValue december52014 = service.portfolioValue(portfolio, dateFromDashString('2014-12-05'))

        then: 'the portfolio value is correct'
        // EBF $16.58, MSFT $29.63
        // (16.58 * 17) + (29.63 * 2) + 457.33
        march172010.marketValue() == toMoney('$798.45')
        // EBF $13.74, MSFT $48.42
        // (13.74 * 17) + (48.42 * 2) + 457.33
        december52014.marketValue() == toMoney('$787.75')
    }
}
