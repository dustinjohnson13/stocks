package com.jdom.bodycomposition.service

import com.jdom.bodycomposition.domain.algorithm.BuyTransaction
import com.jdom.bodycomposition.domain.algorithm.Portfolio
import com.jdom.bodycomposition.domain.algorithm.PortfolioValue
import com.jdom.bodycomposition.domain.algorithm.Position
import com.jdom.bodycomposition.domain.algorithm.PositionValue
import com.jdom.bodycomposition.domain.algorithm.SellTransaction
import com.jdom.bodycomposition.domain.algorithm.impl.TestMsftAlgorithm
import com.jdom.bodycomposition.domain.market.MarketReplay
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.transaction.TransactionConfiguration
import spock.lang.Specification

import javax.transaction.Transactional

import static com.jdom.util.MathUtil.toMoney
import static com.jdom.util.MathUtil.toPercentage
import static com.jdom.util.TimeUtil.dateFromDashString
/**
 * Created by djohnson on 12/12/14.
 */
@ActiveProfiles(SpringProfiles.TEST)
@ContextConfiguration(classes = [StocksServiceContext.class])
@Transactional
@TransactionConfiguration(defaultRollback = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SimpleMarketReplayServiceSpec extends Specification {

    @Autowired
    MarketReplayService service

    @Autowired
    StockDao stockDao

    def msft

    def setup() {
        msft = stockDao.findBySymbol('MSFT')
    }

    def 'should be able to profile algorithms'() {

        def dateOfFirstPurchase = dateFromDashString('2003-11-28')
        def dateOfFirstSale = dateFromDashString('2004-03-18')
        def dateWithSixShares = dateFromDashString('2010-07-15')

        def expectedTransactions = [
                new BuyTransaction(msft, dateOfFirstPurchase, 2, toMoney('$25.50'), toMoney('$4.95')),
                new SellTransaction(msft, dateOfFirstSale, 2, toMoney('$24.89'), toMoney('$4.95')),
                new BuyTransaction(msft, dateFromDashString('2005-05-18'), 2, toMoney('$25.50'), toMoney('$4.95')),
                new BuyTransaction(msft, dateFromDashString('2010-07-14'), 2, toMoney('$25.50'), toMoney('$4.95')),
                new BuyTransaction(msft, dateWithSixShares, 2, toMoney('$25.50'), toMoney('$4.95')),
                new SellTransaction(msft, dateFromDashString('2010-07-16'), 2, toMoney('$24.89'), toMoney('$4.95'))
        ]

        given: 'a portfolio of $200'
        Portfolio initialPortfolio = new Portfolio(toMoney('$200'))

        and: 'purchasing/selling MSFT stock between 11/27/2003 and 07/16/2010'
        MarketReplay scenario = new MarketReplay(initialPortfolio: initialPortfolio,
                algorithm: new TestMsftAlgorithm(),
                startDate: dateFromDashString('2003-11-26'),
                endDate: dateFromDashString('2010-07-16'))

        and: 'a commission cost of $4.95'
        scenario.commissionCost = toMoney('$4.95')

        when: 'the algorithm is profiled'
        MarketReplay result = service.profileAlgorithm(scenario)
        PortfolioValue resultPortfolio = result.resultPortfolio

        then: 'the result contains the correct market value'
        result.resultPortfolio.marketValue() == toMoney('$165.42')

        then: 'the result contains the correct amount of cash'
        resultPortfolio.cash == toMoney('$65.86')

        def positions = resultPortfolio.positions
        and: 'the result contains the correct number of positions'
        positions.size() == 1

        def position = positions.iterator().next()
        and: 'the position has the correct attributes'
        position.security == msft
        position.shares == 4

        def transactions = result.transactions
        and: 'the correct number of transactions exist'
        transactions.size() == 6

        and: 'the transactions have the correct attributes'
        transactions == expectedTransactions

        and: 'the portfolio value change is correct'
        result.valueChangePercent == toPercentage('-17.29%')

        and: 'the dailyPortfolios have the correct attributes'
        def afterFirstPurchasePosition = new Position(msft, 2)
        def afterFirstPurchase = result.dailyPortfolios.find{ it.date == dateOfFirstPurchase }
        afterFirstPurchase != null
        afterFirstPurchase == new PortfolioValue(new Portfolio(toMoney('$144.05'), [afterFirstPurchasePosition] as Set),
                dateOfFirstPurchase, [new PositionValue(afterFirstPurchasePosition, dateOfFirstPurchase, toMoney('$25.71'))] as Set)

        def afterFirstSale = result.dailyPortfolios.find{ it.date == dateOfFirstSale }
        afterFirstSale != null
        afterFirstSale == new PortfolioValue(new Portfolio(toMoney('$188.88'), [] as Set),
                dateOfFirstSale, [] as Set)

        def afterSixSharesPosition = new Position(msft, 6)
        def afterSixShares = result.dailyPortfolios.find{ it.date == dateWithSixShares }
        afterSixShares != null
        afterSixShares == new PortfolioValue(new Portfolio(toMoney('$21.03'), [afterSixSharesPosition] as Set),
                dateWithSixShares, [new PositionValue(afterSixSharesPosition, dateWithSixShares, toMoney('$25.51'))] as Set)
    }
}
