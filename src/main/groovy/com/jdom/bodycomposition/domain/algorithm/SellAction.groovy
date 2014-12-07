package com.jdom.bodycomposition.domain.algorithm
import com.jdom.bodycomposition.domain.YahooStockTicker
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
/**
 * Created by djohnson on 12/6/14.
 */
@ToString(includeSuper = true)
@EqualsAndHashCode(callSuper = true)
class SellAction extends TransferAction {

    SellAction(YahooStockTicker ticker, int numberOfShares, long price) {
        super(ticker, numberOfShares, price)
    }

    @Override
    protected Portfolio createNewPortfolio(Portfolio existing) {
        int actualShareCount = 0
        def shares = [:]
        shares.putAll(existing.shares)

        if (shares.containsKey(ticker)) {
            actualShareCount = shares[ticker]
        }

        def newShareCount = actualShareCount - numberOfShares
        shares.put(ticker, newShareCount)

        def newCash = existing.cash + (price * numberOfShares) - existing.commissionCost
        def newPortfolio = new Portfolio(newCash, existing.commissionCost, shares)

        return newPortfolio
    }

    @Override
    protected String getAction() {
        return "Sold"
    }
}
