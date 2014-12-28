package com.jdom.bodycomposition.domain.algorithm.impl
import com.jdom.bodycomposition.domain.BaseSecurity
import com.jdom.bodycomposition.domain.DailySecurityData
import com.jdom.bodycomposition.domain.DailySecurityMetrics
import com.jdom.bodycomposition.domain.algorithm.Algorithm
import com.jdom.bodycomposition.domain.algorithm.BuyTransaction
import com.jdom.bodycomposition.domain.algorithm.Portfolio
import com.jdom.bodycomposition.domain.algorithm.PortfolioValue
import com.jdom.bodycomposition.domain.algorithm.Position
import com.jdom.bodycomposition.domain.broker.Broker
import com.jdom.bodycomposition.domain.market.orders.Duration
import com.jdom.bodycomposition.domain.market.orders.Order
import com.jdom.bodycomposition.domain.market.orders.OrderRejectedException
import com.jdom.bodycomposition.domain.market.orders.Orders
import com.jdom.util.MathUtil
import com.jdom.util.TimeUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static com.jdom.util.MathUtil.formatPercentage
import static com.jdom.util.MathUtil.toMoney
import static com.jdom.util.MathUtil.toPercentage
/**
 * Created by djohnson on 12/6/14.
 */
abstract class BaseAlgorithm implements Algorithm {
    private static final Logger log = LoggerFactory.getLogger(BaseAlgorithm)

    static final int MAX_NUMBER_OF_POSITIONS_TO_HOLD = 9
    static final int MAXIMUM_ORDER_SEARCHES_TO_TRY_PER_DAY = 10000
    static final int EXCLUSIVE_LOW_VOLUME_REQUIRED = 250000

    def random = new Random()

    @Override
    boolean includeSecurity(final BaseSecurity ticker) {
        return true
    }

    @Override
    void actionsForDay(Broker broker, final PortfolioValue portfolioValue, final List<DailySecurityData> dayEntries,
                       List<DailySecurityMetrics> dailyMetrics, Date currentDate) {

        log.info "Action for day: ${TimeUtil.dashString(currentDate)}"

        if (dayEntries.isEmpty()) {
            log.info "Done with the day"
            return
        }

        def dayEntriesMatchingBuyCriteria = getDayEntriesMatchingBuyCriteria(broker, portfolioValue, dayEntries, dailyMetrics, currentDate)

        Portfolio portfolio = broker.portfolio
        def positions = portfolio.positions
        def transactions = broker.transactions

        if (positions.size() < MAX_NUMBER_OF_POSITIONS_TO_HOLD) {

            log.info "Found ${dayEntriesMatchingBuyCriteria.size()} stocks matching buy criteria."

            boolean submittedOrder = false
            int orderAttempt = 0;
            while (!submittedOrder && orderAttempt++ < MAXIMUM_ORDER_SEARCHES_TO_TRY_PER_DAY && !dayEntriesMatchingBuyCriteria.isEmpty()) {

                DailySecurityData entry = dayEntriesMatchingBuyCriteria.get(random.nextInt(dayEntriesMatchingBuyCriteria.size()))
                if (entry.high < toMoney('$5') || entry.volume < EXCLUSIVE_LOW_VOLUME_REQUIRED) {
                    // Skip really low valued and low volume stocks
                    continue
                }

                int sharesToBuy = BaseAlgorithm.sharesForPortfolioPercentage(broker, portfolioValue, entry.close, toPercentage('10%'))
                long requiredCash = entry.close * sharesToBuy

                if (sharesToBuy > 0 && portfolioValue.cash > requiredCash) {
                    try {
                        log.info "Buying: ${sharesToBuy} shares of ${entry.security.symbol}"
                        broker.submit(Orders.newBuyLimitOrder(sharesToBuy, entry.security, entry.close, Duration.DAY_ORDER))
                        submittedOrder = true
                    } catch (OrderRejectedException e) {
                        log.error("Order rejected", e)
                    }
                }
            }
        }

        for (Position position : positions) {
            def entry = dayEntries.find { it.security == position.security }
            if (entry == null) {
                continue
            }
            def purchase = transactions.find {
                it instanceof BuyTransaction && it.security == position.security
            }
            if (purchase == null) {
                log.error "Could not find purchase transaction for ${entry.security}"
                continue
            }
            def metrics = dailyMetrics.find { it.fiftyTwoWeekHigh.security == position.security }
            if (metrics == null) {
                log.error "Could not find daily metrics for ${entry.security}"
                continue
            }

            if (portfolio.cash >= broker.commissionCost) {
                Order sellOrder = checkForSellPosition(position, entry, metrics, purchase)
                def percentGain = MathUtil.percentChange(entry.close, purchase.price)
                if (sellOrder != null) {
                    log.info "Bought ${entry.security} at ${purchase.price}, current price ${entry.close}, percent change: ${formatPercentage(percentGain)} (${percentGain}).  Selling successful trade!"
                } else if (percentGain < (-getMaxAllowedPercentageLossPerTrade())) {
                    log.info "Bought ${entry.security} at ${purchase.price}, current price ${entry.close}, percent change: ${formatPercentage(percentGain)} (${percentGain}).  Selling failed trade!"
                    sellOrder = Orders.newSellLimitOrder(position.shares, position.security, entry.close, Duration.DAY_ORDER)
                }

                if (sellOrder != null) {
                    try {
                        broker.submit(sellOrder)
                    } catch (OrderRejectedException e) {
                        log.error("Order rejected", e)
                    }
                }
            }
        }

        log.info "Done with the day"
    }

    protected abstract long getMaxAllowedPercentageLossPerTrade()

    protected abstract List<DailySecurityData> getDayEntriesMatchingBuyCriteria(
          final Broker broker, final PortfolioValue portfolioValue, final List<DailySecurityData> dailySecurityDatas,
          final List<DailySecurityMetrics> dailySecurityMetrics, final Date date)

    protected abstract Order checkForSellPosition(final Position position, final DailySecurityData entry, final DailySecurityMetrics metrics, final BuyTransaction purchase)

    static int sharesForPortfolioPercentage(
          final Broker broker, final PortfolioValue portfolioValue, final long price, final long percentage) {
        long marketValue = portfolioValue.marketValue()
        long percentageCashValue = marketValue * (percentage / 10000)
        long minusCommissionCost = percentageCashValue - broker.commissionCost

        return minusCommissionCost / price
    }
}