package com.jdom.bodycomposition.domain.algorithm

import com.jdom.bodycomposition.domain.YahooStockTicker
import com.jdom.util.MathUtil
import com.jdom.util.TimeUtil
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by djohnson on 12/6/14.
 */
@ToString
@EqualsAndHashCode
abstract class TransferTransaction implements PortfolioTransaction {
    private static Logger log = LoggerFactory.getLogger(SellTransaction)

    final YahooStockTicker ticker
    final Date date
    final int numberOfShares
    final long price
    final long commission

    TransferTransaction(YahooStockTicker ticker, final Date date, int numberOfShares, long price, long commission) {
        this.ticker = ticker
        this.date = date
        this.numberOfShares = numberOfShares
        this.price = price
        this.commission = commission
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
            StringBuilder sb = new StringBuilder(TimeUtil.dashString(date)).append(getAction()).append(" ")
                    .append(ticker.ticker).append(" ")
                    .append(numberOfShares).append("@").append(MathUtil.formatMoney(price)).append(":\n")
            sb.append("   From portfolio: ${portfolio}\n").append("   New portfolio: ${newPortfolio}")

            log.info(sb.toString())
        }

        return newPortfolio
    }

    @Override
    String getSymbol() {
        return ticker.ticker
    }

    @Override
    int getShares() {
        return numberOfShares
    }

    @Override
    long getCommission() {
        return commission
    }

    protected abstract Portfolio createNewPortfolio(Portfolio existing)
}
