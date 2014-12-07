package com.jdom.bodycomposition.domain.algorithm
import com.jdom.bodycomposition.domain.YahooStockTicker
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
/**
 * Created by djohnson on 12/6/14.
 */
@ToString(includeSuper = true)
@EqualsAndHashCode(callSuper = true)
class SellTransaction extends TransferTransaction {

    SellTransaction(YahooStockTicker ticker, Date date, int numberOfShares, long price, long commission) {
        super(ticker, date, numberOfShares, price, commission)
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

        long newCash = existing.cash - cashValue - commission

        def newPortfolio = new Portfolio(newCash, existing.commissionCost, shares)

        return newPortfolio
    }

    @Override
    public String getAction() {
        return "Sell"
    }

    @Override
    long getCashValue() {
        return price * numberOfShares
    }
}
