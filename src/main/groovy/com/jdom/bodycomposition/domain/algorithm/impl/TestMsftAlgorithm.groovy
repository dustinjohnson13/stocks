package com.jdom.bodycomposition.domain.algorithm.impl

import com.jdom.bodycomposition.domain.BaseSecurity
import com.jdom.bodycomposition.domain.DailySecurityData
import com.jdom.bodycomposition.domain.DailySecurityMetrics
import com.jdom.bodycomposition.domain.algorithm.Algorithm
import com.jdom.bodycomposition.domain.algorithm.PortfolioValue
import com.jdom.bodycomposition.domain.broker.Broker
import com.jdom.bodycomposition.domain.market.orders.Duration
import com.jdom.bodycomposition.domain.market.orders.Orders
import com.jdom.util.TimeUtil

/**
 * Created by djohnson on 12/6/14.
 */
class TestMsftAlgorithm implements Algorithm {

    def purchaseDates = ['2003-11-26', '2005-05-17', '2010-07-13', '2010-07-14']
    def sellDates = ['2004-03-17', '2010-07-15', '2011-09-29']

    @Override
    boolean includeSecurity(final BaseSecurity ticker) {
        return ticker.symbol == 'MSFT'
    }

    @Override
    void actionsForDay(final Broker broker, final PortfolioValue portfolioValue, final List<DailySecurityData> dayEntries,
                       List<DailySecurityMetrics> dailyMetrics, Date currentDate) {
        if (purchaseDates.contains(TimeUtil.dashString(currentDate))) {
            broker.submit(Orders.newBuyLimitOrder(2, dayEntries.find{ it.security.symbol == 'MSFT' }.security, 2550, Duration.GTC)) // GTC to work around thanksgiving
        } else if (sellDates.contains(TimeUtil.dashString(currentDate))) {
            broker.submit(Orders.newSellLimitOrder(2, dayEntries.find{ it.security.symbol == 'MSFT' }.security, 2489, Duration.DAY_ORDER))
        }
    }
}
