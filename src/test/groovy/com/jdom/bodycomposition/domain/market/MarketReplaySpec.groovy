package com.jdom.bodycomposition.domain.market
import com.jdom.bodycomposition.domain.BaseSecurity
import com.jdom.bodycomposition.domain.DailySecurityData
import com.jdom.bodycomposition.domain.Stock
import com.jdom.bodycomposition.domain.algorithm.Algorithm
import com.jdom.bodycomposition.domain.algorithm.Portfolio
import com.jdom.bodycomposition.domain.algorithm.PortfolioValue
import com.jdom.bodycomposition.domain.broker.Broker
import com.jdom.bodycomposition.service.DailySecurityDataDao
import com.jdom.bodycomposition.service.SecurityService
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import static com.jdom.util.TimeUtil.dateFromDashString
/**
 * Created by djohnson on 12/11/14.
 */
class MarketReplaySpec extends Specification {

    @Shared
    BaseSecurity msft = new Stock(id: 1L, symbol: 'MSFT')
    @Shared
    BaseSecurity hp = new Stock(id: 2L, symbol: 'HP')
    @Shared
    BaseSecurity fb = new Stock(id: 3L, symbol: 'FB')

    @Unroll
    def 'should replay each day in range'() {

        def startDateString = '2010-01-01'
        def startDate = dateFromDashString(startDateString)

        def endDateString = '2010-01-08'
        def endDate = dateFromDashString(endDateString)

        Portfolio initialPortfolio = new Portfolio(20000l)
        MarketEngine marketEngine = Mock()
        _ * marketEngine.getPortfolio() >> initialPortfolio
        _ * marketEngine.getTransactions() >> []

        def expectedReplayDate = dateFromDashString(expectedReplayDateString)

        Algorithm algorithm = Mock()
        1 * algorithm.includeSecurity(msft) >> true
        1 * algorithm.includeSecurity(hp) >> true
        1 * algorithm.includeSecurity(fb) >> false

        SecurityService securityService = Mock()
        1 * securityService.getStocks() >> [msft, hp, fb]

        def portfolioValue = new PortfolioValue(initialPortfolio, startDate, [] as Set)
        _ * securityService.portfolioValue(_ as Portfolio, _ as Date) >> portfolioValue

        DailySecurityDataDao dailySecurityDataDao = Mock()
        1 * dailySecurityDataDao.findBySecurityInAndDate([msft, hp], expectedReplayDate) >> expectedDailySecurityData

        given:
        "a market replay configuration from ${startDateString} to ${endDateString}"
        MarketReplay replay = new MarketReplay() {
            @Override
            protected MarketEngine createMarketEngine(final DailySecurityDataDao dao) {
                return marketEngine
            }
        }
        replay.initialPortfolio = initialPortfolio
        replay.algorithm = algorithm
        replay.startDate = startDate
        replay.endDate = endDate

        when: 'the market is replayed'
        replay.replay(dailySecurityDataDao, securityService)

        then: 'the day is replayed against the market engine first'
        1 * marketEngine.processDay(expectedReplayDate)

        then: 'the day is replayed against the algorithm second with the daily security data entries for that date'
        1 * algorithm.actionsForDay(_ as Broker, portfolioValue, expectedDailySecurityData, expectedReplayDate)

        where:
        expectedReplayDateString | expectedDailySecurityData
        '2010-01-01'             | []
        '2010-01-02'             | [new DailySecurityData(id: 1L, security: msft), new DailySecurityData(id: 2L, security: hp)]
        '2010-01-03'             | [new DailySecurityData(id: 3L, security: msft), new DailySecurityData(id: 4L, security: hp)]
        '2010-01-04'             | [new DailySecurityData(id: 5L, security: msft), new DailySecurityData(id: 6L, security: hp)]
        '2010-01-05'             | [new DailySecurityData(id: 7L, security: msft), new DailySecurityData(id: 8L, security: hp)]
        '2010-01-06'             | [new DailySecurityData(id: 9L, security: msft), new DailySecurityData(id: 10L, security: hp)]
        '2010-01-07'             | [new DailySecurityData(id: 11L, security: msft), new DailySecurityData(id: 12L, security: hp)]
        '2010-01-08'             | [new DailySecurityData(id: 13L, security: msft), new DailySecurityData(id: 14L, security: hp)]
    }
}
