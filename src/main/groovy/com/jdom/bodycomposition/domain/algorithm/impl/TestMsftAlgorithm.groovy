package com.jdom.bodycomposition.domain.algorithm.impl
import com.jdom.bodycomposition.domain.BaseSecurity
import com.jdom.bodycomposition.domain.DailySecurityData
import com.jdom.bodycomposition.domain.algorithm.Algorithm
import com.jdom.bodycomposition.domain.market.Market
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
    void actionsForDay(Market market, final List<DailySecurityData> dayEntries, Date currentDate) {
        if (purchaseDates.contains(TimeUtil.dashString(currentDate))) {
            market.submit(Orders.newBuyLimitOrder(2, dayEntries.find{ it.security.symbol == 'MSFT' }.security, 2550, Duration.GTC)) // GTC to work around thanksgiving
//            return [new BuyTransaction(dayEntry.security, dayEntry.date, 2, 2550, portfolio.commissionCost)]
        } else if (sellDates.contains(TimeUtil.dashString(currentDate))) {
            market.submit(Orders.newSellLimitOrder(2, dayEntries.find{ it.security.symbol == 'MSFT' }.security, 2489, Duration.DAY_ORDER))
//            return [new SellTransaction(dayEntry.security, dayEntry.date, 2, 2489, portfolio.commissionCost)]
        }
    }
}
