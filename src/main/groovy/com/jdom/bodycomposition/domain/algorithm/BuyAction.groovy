package com.jdom.bodycomposition.domain.algorithm
import com.jdom.bodycomposition.domain.YahooStockTicker
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
/**
 * Created by djohnson on 12/6/14.
 */
@ToString(includeSuper = true)
@EqualsAndHashCode(callSuper = true)
class BuyAction extends TransferAction {

    BuyAction(YahooStockTicker ticker, int numberOfShares, long price) {
        super(ticker, numberOfShares, price)
    }

    @Override
    protected Portfolio createNewPortfolio(Portfolio existing) {
        long newCash = existing.cash - (price * numberOfShares) - existing.commissionCost

        def shares = [:]
        shares.putAll(existing.shares)

        def shareCount = shares.containsKey(ticker) ? shares.get(ticker) + numberOfShares : numberOfShares
        shares[ticker] = shareCount

        return new Portfolio(newCash, existing.commissionCost, shares)
    }

    @Override
    protected String getAction() {
        return "Purchased"
    }
}
