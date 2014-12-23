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
import com.jdom.util.MathUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static com.jdom.util.MathUtil.toPercentage

/**
 * Created by djohnson on 12/6/14.
 */
class BuyAt52WeekLowSellAtGivenPercentUpOrDown extends BaseAlgorithm {
    private static final Logger log = LoggerFactory.getLogger(BuyAt52WeekLowSellAtGivenPercentUpOrDown)

    static final long PERCENTAGE_PROFIT_OR_LOSS_TO_SELL_POSITION_AT = toPercentage('7%')

    @Override
    protected long getMaxAllowedPercentageLossPerTrade() {
        return PERCENTAGE_PROFIT_OR_LOSS_TO_SELL_POSITION_AT
    }

    @Override
    protected List<DailySecurityData> getDayEntriesMatchingBuyCriteria(
          final Broker broker,
          final PortfolioValue portfolioValue,
          final List<DailySecurityData> dailySecurityDatas,
          final List<DailySecurityMetrics> dailySecurityMetrics, final Date date) {
        Set<Long> fiftyTwoWeekLowIds = new HashSet<>(dailySecurityMetrics.collect { it.fiftyTwoWeekLow.id })
        Set<Long> todayIds = new HashSet<>(dailySecurityDatas.collect { it.id })

        def intersection = todayIds.intersect(fiftyTwoWeekLowIds)

        log.info "Found ${intersection.size()} 52 week lows for today."

        return dailySecurityDatas.findAll { intersection.contains(it.id) }
    }

    @Override
    protected Order checkForSellPosition(
          final Position position, final DailySecurityData entry, final DailySecurityMetrics metrics, final BuyTransaction purchase) {
        def percentGain = MathUtil.percentChange(entry.close, purchase.price)

        if (percentGain > PERCENTAGE_PROFIT_OR_LOSS_TO_SELL_POSITION_AT) {
            return Orders.newSellLimitOrder(position.shares, position.security, entry.close, Duration.DAY_ORDER)
        }
        return null
    }

}
