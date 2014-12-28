package com.jdom.bodycomposition.service

import com.jdom.bodycomposition.domain.market.MarketReplay
import com.jdom.util.TimeUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
/**
 * Created by djohnson on 11/14/14.
 */
@Service
class SimpleMarketReplayService implements MarketReplayService {

    private static final Logger log = LoggerFactory.getLogger(SimpleMarketReplayService)

    private static final int instancesOfAlgorithms = 100

    @Autowired
    SecurityService securityService

    @Autowired
    DailySecurityDataDao dailySecurityDataDao

    @Autowired
    DailySecurityMetricsDao dailySecurityMetricsDao

    @Override
    MarketReplay profileAlgorithm(final MarketReplay scenario) {

//        def bearDates = [TimeUtil.dateFromDashString('2008-03-06'), TimeUtil.dateFromDashString('2009-03-06')]
//        def bullDates = [TimeUtil.dateFromDashString('2012-06-01'), TimeUtil.dateFromDashString('2013-06-01')]
        def neitherDates = [TimeUtil.dateFromDashString('2004-12-17'), TimeUtil.dateFromDashString('2005-12-17')]
//        def neitherDates = [TimeUtil.dateFromDashString('2004-12-17'), TimeUtil.dateFromDashString('2004-12-23')]
//        'Bear': bearDates, 'Bull': bullDates,

        def marketTypes = ['Neither': neitherDates]

        return profileAlgorithm(scenario, marketTypes)
    }

    MarketReplay profileAlgorithm(final MarketReplay scenario, final Map<String, List<Date>> marketTypes) {

        marketTypes.each { type, dates ->

            def startDate = dates[0]
            def endDate = dates[1]

            scenario.replay(startDate, endDate, dailySecurityDataDao, dailySecurityMetricsDao, securityService, instancesOfAlgorithms)
        }

        return scenario
    }

}
