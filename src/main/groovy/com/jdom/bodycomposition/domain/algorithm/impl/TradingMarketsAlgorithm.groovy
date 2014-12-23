package com.jdom.bodycomposition.domain.algorithm.impl
import com.jdom.bodycomposition.domain.DailySecurityData
import com.jdom.bodycomposition.domain.DailySecurityMetrics
import com.jdom.bodycomposition.domain.algorithm.BuyTransaction
import com.jdom.bodycomposition.domain.algorithm.PortfolioValue
import com.jdom.bodycomposition.domain.algorithm.Position
import com.jdom.bodycomposition.domain.broker.Broker
import com.jdom.bodycomposition.domain.market.orders.Duration
import com.jdom.bodycomposition.domain.market.orders.Order
import com.jdom.bodycomposition.domain.market.orders.Orders
import com.jdom.util.TimeUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static com.jdom.util.MathUtil.toPercentage
/**
 * Created by djohnson on 12/6/14.
 */
class TradingMarketsAlgorithm extends BaseAlgorithm {
    private static final Logger log = LoggerFactory.getLogger(TradingMarketsAlgorithm)

    static final long PERCENTAGE_LOSS_TO_SELL_POSITION_AT = toPercentage('7%')

    @Override
    protected List<DailySecurityData> getDayEntriesMatchingBuyCriteria(
          final Broker broker,
          final PortfolioValue portfolioValue,
          final List<DailySecurityData> dailySecurityDatas,
          final List<DailySecurityMetrics> dailySecurityMetrics, final Date date) {

        def above200DayAverage = []
        for (DailySecurityData dayEntry : dailySecurityDatas) {

            DailySecurityMetrics metrics = dailySecurityMetrics.find{ it.fiftyTwoWeekHigh.security == dayEntry.security }
            if (metrics == null) {
                LoggerFactory.getLogger(TradingMarketsAlgorithm.class).error("Unable to find daily metrics for security ${dayEntry.security} on date ${TimeUtil.dashString(dayEntry.date)}")
                continue;
            }
            if (dayEntry.low > metrics.twoHundredDaySimpleMovingAverage) {
                above200DayAverage.add(dayEntry)
            }
        }

        return above200DayAverage
    }

    @Override
    protected Order checkForSellPosition(
          final Position position, final DailySecurityData entry, final DailySecurityMetrics metrics, final BuyTransaction purchase) {

        if (entry.close > metrics.fiveDaySimpleMovingAverage) {
            return Orders.newSellLimitOrder(position.shares, position.security, entry.close, Duration.DAY_ORDER)
        }

        return null
    }

    @Override
    protected long getMaxAllowedPercentageLossPerTrade() {
        return PERCENTAGE_LOSS_TO_SELL_POSITION_AT
    }
}
