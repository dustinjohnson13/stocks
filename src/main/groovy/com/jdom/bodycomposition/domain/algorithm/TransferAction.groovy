package com.jdom.bodycomposition.domain.algorithm

import com.jdom.bodycomposition.domain.YahooStockTicker
import com.jdom.util.MathUtil
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by djohnson on 12/6/14.
 */
@ToString
@EqualsAndHashCode
abstract class TransferAction implements TickerAction {
    private static Logger log = LoggerFactory.getLogger(SellAction)

    final YahooStockTicker ticker
    final int numberOfShares
    final long price

    TransferAction(YahooStockTicker ticker, int numberOfShares, long price) {
        this.ticker = ticker
        this.numberOfShares = numberOfShares
        this.price = price
    }

    @Override
    Portfolio apply(final Portfolio portfolio) {

        def newPortfolio = createNewPortfolio(portfolio)
        if (newPortfolio.cash < 0) {
            throw new IllegalArgumentException("Portfolio would have ${newPortfolio.cash}, unable to apply!")
        }
        newPortfolio.shares.each { ticker, shares ->
            if (shares < 0) {
                throw new IllegalArgumentException("Portfolio would have ${shares} shares of [${ticker}], " +
                    "unable to apply!")
            }
        }

        if (log.isInfoEnabled()) {
            StringBuilder sb = new StringBuilder(getAction()).append(" ").append(ticker.ticker).append(" ")
                    .append(numberOfShares).append("@").append(MathUtil.formatMoney(price)).append(":\n")
            sb.append("   From portfolio: ${portfolio}\n").append("   New portfolio: ${newPortfolio}")

            log.info(sb.toString())
        }

        return newPortfolio
    }

    protected abstract Portfolio createNewPortfolio(Portfolio existing)

    protected abstract String getAction()
}
