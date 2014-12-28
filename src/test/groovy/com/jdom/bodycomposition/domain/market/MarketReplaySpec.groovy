package com.jdom.bodycomposition.domain.market
import com.jdom.bodycomposition.domain.BaseSecurity
import com.jdom.bodycomposition.domain.DailySecurityData
import com.jdom.bodycomposition.domain.Stock
import com.jdom.bodycomposition.domain.algorithm.Algorithm
import com.jdom.bodycomposition.domain.algorithm.Portfolio
import com.jdom.bodycomposition.domain.algorithm.PortfolioValue
import com.jdom.bodycomposition.domain.broker.Broker
import com.jdom.bodycomposition.service.DailySecurityDataDao
import com.jdom.bodycomposition.service.DailySecurityMetricsDao
import com.jdom.bodycomposition.service.SecurityService
import spock.lang.Shared
import spock.lang.Specification

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

    def 'should replay each day in range'() {

        def startDateString = '2010-01-01'
        def startDate = dateFromDashString(startDateString)

        def endDateString = '2010-01-08'
        def endDate = dateFromDashString(endDateString)

        Portfolio initialPortfolio = Portfolio.newPortfolio(20000l)
        MarketEngine marketEngine = Mock()
        _ * marketEngine.getPortfolio() >> initialPortfolio
        _ * marketEngine.getTransactions() >> []

        Algorithm algorithm = Mock()
        Algorithm algorithm2 = Mock()

        SecurityService securityService = Mock()

        def portfolioValue = PortfolioValue.newPortfolioValue(initialPortfolio, startDate, [] as Set)
        _ * securityService.portfolioValue(_ as Portfolio, _ as Date, _ as Map<Integer, DailySecurityData>) >> portfolioValue

        def datesToData = new LinkedHashMap<Date, List<DailySecurityData>>()
        datesToData.put(dateFromDashString('2010-01-01'), [])
        datesToData.put(dateFromDashString('2010-01-02'), [new DailySecurityData(id: 1L, security: msft), new DailySecurityData(id: 2L, security: hp)])
        datesToData.put(dateFromDashString('2010-01-03'), [new DailySecurityData(id: 3L, security: msft), new DailySecurityData(id: 4L, security: hp)])
        datesToData.put(dateFromDashString('2010-01-04'), [new DailySecurityData(id: 5L, security: msft), new DailySecurityData(id: 6L, security: hp)])
        datesToData.put(dateFromDashString('2010-01-05'), [new DailySecurityData(id: 7L, security: msft), new DailySecurityData(id: 8L, security: hp)])
        datesToData.put(dateFromDashString('2010-01-06'), [new DailySecurityData(id: 9L, security: msft), new DailySecurityData(id: 10L, security: hp)])
        datesToData.put(dateFromDashString('2010-01-07'), [new DailySecurityData(id: 11L, security: msft), new DailySecurityData(id: 12L, security: hp)])
        datesToData.put(dateFromDashString('2010-01-08'), [new DailySecurityData(id: 13L, security: msft), new DailySecurityData(id: 14L, security: hp)])

        DailySecurityDataDao dailySecurityDataDao = Mock()
        datesToData.each { date, data ->
            println date
            1 * dailySecurityDataDao.findByDate(date) >> data
        }

        DailySecurityMetricsDao dailySecurityMetricsDao = Mock()
        datesToData.each { date, data ->
            1 * dailySecurityMetricsDao.findByDate(date) >> []
        }

        def expectedMaps = [:]
        datesToData.each { date, data ->
            def map = MarketReplay.createMapOfDailySecurityDataBySecurityId(data)
            expectedMaps.put(date, map)
        }

        given:
        "a market replay configuration from ${startDateString} to ${endDateString}"
        MarketReplay replay = new MarketReplay() {
            @Override
            protected MarketEngine createMarketEngine(final DailySecurityDataDao dao) {
                return marketEngine
            }
        }
        replay.initialPortfolio = initialPortfolio
        replay.algorithmFactory = new AlgorithmFactory() {
            @Override
            Algorithm createInstance(final int identifier) {
                return (identifier == 1) ? algorithm : algorithm2
            }
        }

        when: 'the market is replayed'
        replay.replay(startDate, endDate, dailySecurityDataDao, dailySecurityMetricsDao, securityService, 2)

        then: 'the day is replayed against the market engine first then against the algorithms'
        datesToData.each { date, data ->
            def expectedMap = expectedMaps.get(date)
            1 * marketEngine.processDay(date, expectedMap)
        }

        and: 'the day is replayed against the algorithms'
        datesToData.each { date, data ->
            if (data.empty) {
                0 * algorithm.actionsForDay(_ as Broker, portfolioValue, data, [], date)
                0 * algorithm2.actionsForDay(_ as Broker, portfolioValue, data, [], date)
            } else {
                1 * algorithm.actionsForDay(_ as Broker, portfolioValue, data, [], date)
                1 * algorithm2.actionsForDay(_ as Broker, portfolioValue, data, [], date)
            }
        }
    }
}
