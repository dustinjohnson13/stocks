package com.jdom.bodycomposition.domain.algorithm
import com.jdom.bodycomposition.domain.YahooStockTicker
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
/**
 * Created by djohnson on 12/6/14.
 */
@ToString(includeSuper = true)
@EqualsAndHashCode(callSuper = true)
class BuyTransaction extends TransferTransaction {

    BuyTransaction(YahooStockTicker ticker, Date date, int numberOfShares, long price, long commission) {
        super(ticker, date, numberOfShares, price, commission)
    }

    @Override
    protected Portfolio createNewPortfolio(Portfolio existing) {
        long newCash = existing.cash + cashValue - commission

        def shares = [:]
        shares.putAll(existing.shares)

        def shareCount = shares.containsKey(ticker) ? shares.get(ticker) + numberOfShares : numberOfShares
        shares[ticker] = shareCount

        return new Portfolio(newCash, existing.commissionCost, shares)
    }

    @Override
    public String getAction() {
        return "Buy"
    }

    @Override
    long getCashValue() {
        return -(price * numberOfShares)
    }
}
