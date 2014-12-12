package com.jdom.bodycomposition.service

import com.jdom.bodycomposition.domain.market.MarketReplay
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

    @Autowired
    SecurityService securityService

    @Autowired
    DailySecurityDataDao dailySecurityDataDao

    @Override
    MarketReplay profileAlgorithm(final MarketReplay scenario) {

        scenario.replay(dailySecurityDataDao, securityService)

        return scenario
    }

}
