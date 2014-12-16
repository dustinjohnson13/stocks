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
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.ArrayBlockingQueue

import static com.jdom.util.MathUtil.toPercentage
/**
 * Created by djohnson on 12/6/14.
 */
class TradingMarketsAlgorithm extends BaseAlgorithm {
    private static final Logger log = LoggerFactory.getLogger(TradingMarketsAlgorithm)

    static final long PERCENTAGE_LOSS_TO_SELL_POSITION_AT = toPercentage('7%')
    public static final int DAYS_INCLUDED_IN_MOVING_AVERAGE = 200

    def averages = [:]
    def daysSeen = 0

    @Override
    protected List<DailySecurityData> getDayEntriesMatchingBuyCriteria(
          final Broker broker,
          final PortfolioValue portfolioValue,
          final List<DailySecurityData> dailySecurityDatas,
          final List<DailySecurityMetrics> dailySecurityMetrics, final Date date) {

        def above200DayAverage = []
        for (DailySecurityData dayEntry : dailySecurityDatas) {
            ArrayBlockingQueue<Long> values = averages.get(dayEntry.security.id)
            if (values == null) {
                values = new ArrayBlockingQueue<>(DAYS_INCLUDED_IN_MOVING_AVERAGE)
                averages.put(dayEntry.security.id, values)
            } else if (values.size() == DAYS_INCLUDED_IN_MOVING_AVERAGE) {
                values.take()
            }

            values.add(dayEntry.high)

            long value = 0;
            values.each {
                value += it
            }

            def average = value / values.size();
            if (dayEntry.low > average) {
                above200DayAverage.add(dayEntry)
            }
        }

        if (daysSeen++ < DAYS_INCLUDED_IN_MOVING_AVERAGE) {
            log.info "Skipping first ${DAYS_INCLUDED_IN_MOVING_AVERAGE} days, currently on day ${daysSeen}."
            return []
        }

        return above200DayAverage
    }

    @Override
    protected Order checkForSellPosition(
          final Position position, final DailySecurityData entry, final BuyTransaction purchase) {

        ArrayBlockingQueue<Long> dailyHighs = averages.get(entry.security.id)
        Long[] dailyHighValues = dailyHighs.toArray(new Long[0])
        def length = dailyHighValues.length
        if (length > 5) {
            Long fiveDayAverage = 0;
            for (int i = length - 1; i--; i > (length - 6)) {
                fiveDayAverage += dailyHighValues[i]
            }
            fiveDayAverage = fiveDayAverage / 5

            if (entry.close > fiveDayAverage) {
                return Orders.newSellLimitOrder(position.shares, position.security, entry.close, Duration.DAY_ORDER)
            }
        }

        return null
    }

    @Override
    protected long getMaxAllowedPercentageLossPerTrade() {
        return PERCENTAGE_LOSS_TO_SELL_POSITION_AT
    }
}
