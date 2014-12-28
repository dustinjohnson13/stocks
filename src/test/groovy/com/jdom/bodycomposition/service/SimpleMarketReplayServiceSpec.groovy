package com.jdom.bodycomposition.service

import com.jdom.bodycomposition.domain.algorithm.Portfolio
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

        given: 'a portfolio of $200'
        Portfolio initialPortfolio = Portfolio.newPortfolio(toMoney('$200'))

        and: 'purchasing/selling MSFT stock between 11/27/2003 and 07/16/2010'
        MarketReplay scenario = new MarketReplay(initialPortfolio: initialPortfolio,
                algorithmFactory: TestMsftAlgorithm.factory)

        and: 'a commission cost of $4.95'
        scenario.commissionCost = toMoney('$4.95')

        when: 'the algorithm is profiled'
        MarketReplay result = service.profileAlgorithm(scenario, ['MSFT specific': [dateFromDashString('2003-11-26'),
                                                                                    dateFromDashString('2010-07-16')]
        ])

        then: 'the portfolio value change is correct'
        result.averagePercentChange == toPercentage('-17.29%')
        result.minPercentChange == toPercentage('-17.29%')
        result.maxPercentChange == toPercentage('-17.29%')

    }
}
