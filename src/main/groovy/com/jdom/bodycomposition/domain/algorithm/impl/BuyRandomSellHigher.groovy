package com.jdom.bodycomposition.domain.algorithm.impl
import com.jdom.bodycomposition.domain.BaseSecurity
import com.jdom.bodycomposition.domain.DailySecurityData
import com.jdom.bodycomposition.domain.algorithm.Algorithm
import com.jdom.bodycomposition.domain.algorithm.BuyTransaction
import com.jdom.bodycomposition.domain.algorithm.Portfolio
import com.jdom.bodycomposition.domain.algorithm.PortfolioValue
import com.jdom.bodycomposition.domain.algorithm.Position
import com.jdom.bodycomposition.domain.broker.Broker
import com.jdom.bodycomposition.domain.market.orders.Duration
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
class BuyRandomSellHigher implements Algorithm {
    private static final Logger log = LoggerFactory.getLogger(BuyRandomSellHigher)

    static final long PERCENTAGE_PROFIT_OR_LOSS_TO_SELL_POSITION_AT = toPercentage('7%')
    static final int MAX_NUMBER_OF_POSITIONS_TO_HOLD = 10
    static final int MAXIMUM_ORDER_SEARCHES_TO_TRY_PER_DAY = 10000
    static final int EXCLUSIVE_LOW_VOLUME_REQUIRED = 250000

    def random = new Random()

    @Override
    boolean includeSecurity(final BaseSecurity ticker) {
        return true
    }

    @Override
    void actionsForDay(Broker broker, final PortfolioValue portfolioValue, final List<DailySecurityData> dayEntries, Date currentDate) {

        log.info "Action for day: ${TimeUtil.dashString(currentDate)}"

        if (dayEntries.isEmpty()) {
            log.info "Done with the day"
            return
        }

        Portfolio portfolio = broker.portfolio
        def positions = portfolio.positions
        def transactions = broker.transactions

        if (positions.size() < MAX_NUMBER_OF_POSITIONS_TO_HOLD) {

            boolean submittedOrder = false
            int orderAttempt = 0;
            while (!submittedOrder && orderAttempt < MAXIMUM_ORDER_SEARCHES_TO_TRY_PER_DAY) {
                DailySecurityData entry = dayEntries.get(random.nextInt(dayEntries.size()))
                if (entry.high < toMoney('$5')) { // Skip really low valued stocks
                    continue
                }
                if ((entry.close == entry.low) && (entry.volume > EXCLUSIVE_LOW_VOLUME_REQUIRED)) { // Find an entry that closed at its low
                    // Find the number of shares that would equal approx. 1/10 of portfolio value
                    def oneTenthOfPortfolioValue = portfolioValue.marketValue() / MAX_NUMBER_OF_POSITIONS_TO_HOLD
                    if (entry.close < oneTenthOfPortfolioValue) {
                        def sharesToBuy = (int) (oneTenthOfPortfolioValue / entry.close)
                        if (sharesToBuy > 0) {
                            try {
                                log.info "Buying: ${sharesToBuy} shares of ${entry.security.symbol}"
                                broker.submit(Orders.newBuyLimitOrder(sharesToBuy, entry.security, entry.close, Duration.DAY_ORDER))
                                submittedOrder = true
                            } catch (OrderRejectedException e) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
                orderAttempt++
            }
        }

        for (Position position : positions) {
            // If any positions have made enough profit, or lost enough money, set a limit order
            def entry = dayEntries.find { it.security == position.security }
            if (entry == null) {
                continue
            }
            def purchase = transactions.find { it instanceof BuyTransaction && it.security == position.security }
            if (purchase != null) {
                def percentGain = MathUtil.percentChange(entry.close, purchase.price)
                if (percentGain > PERCENTAGE_PROFIT_OR_LOSS_TO_SELL_POSITION_AT || percentGain < (-PERCENTAGE_PROFIT_OR_LOSS_TO_SELL_POSITION_AT)) {
                    try {
                        log.info "Bought ${entry.security} at ${purchase.price}, current price ${entry.close}, percent change: ${formatPercentage(percentGain)} (${percentGain}).  Selling!"
                        broker.submit(Orders.newSellLimitOrder(position.shares, position.security, entry.close, Duration.DAY_ORDER))
                    } catch (OrderRejectedException e) {
                        e.printStackTrace()
                    }
                }
            } else {
                log.info "Could not find purchase transaction for ${entry.security}"
            }
        }

        log.info "Done with the day"
    }

}
