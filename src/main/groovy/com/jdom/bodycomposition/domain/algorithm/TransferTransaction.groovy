package com.jdom.bodycomposition.domain.algorithm
import com.jdom.bodycomposition.domain.BaseSecurity
import com.jdom.util.MathUtil
import com.jdom.util.TimeUtil
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static MathUtil.formatMoney

/**
 * Created by djohnson on 12/6/14.
 */
@ToString
@EqualsAndHashCode
abstract class TransferTransaction implements PortfolioTransaction {
    private static Logger log = LoggerFactory.getLogger(TransferTransaction)

    final BaseSecurity security
    final Date date
    final int numberOfShares
    final long price
    final long commission

    TransferTransaction(BaseSecurity security, final Date date, int numberOfShares, long price, long commission) {
        this.security = security
        this.date = date
        this.numberOfShares = numberOfShares
        this.price = price
        this.commission = commission
    }

    @Override
    Portfolio apply(final Portfolio portfolio, boolean logTransaction = true) {

        def newPortfolio = createNewPortfolio(portfolio)
        if (newPortfolio.cash < 0) {
            throw new IllegalArgumentException("Portfolio would have ${formatMoney(newPortfolio.cash)}, unable to apply!")
        }
        newPortfolio.positions.each { position ->
            if (position.shares < 0) {
                throw new IllegalArgumentException("Portfolio would have ${position.shares} shares of [${position.security}], " +
                        "unable to apply!")
            }
        }

        if (logTransaction && log.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder('\n').append(TimeUtil.dashString(date)).append(' ')
                  .append(getAction()).append(" ").append(security.exchange).append(':')
                  .append(security.symbol).append(" ").append(numberOfShares).append("@")
                  .append(MathUtil.formatMoney(price)).append(":\n")
                  sb.append("   From portfolio: ${portfolio}\n").append("   New portfolio: ${newPortfolio}")

            log.debug(sb.toString())
        }

        return newPortfolio
    }

    @Override
    String getSymbol() {
        return security.symbol
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
