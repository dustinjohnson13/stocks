package com.jdom.bodycomposition.domain.algorithm

import com.jdom.bodycomposition.domain.YahooStockTicker
import com.jdom.util.MathUtil

/**
 * Created by djohnson on 12/6/14.
 */
class Portfolio implements Serializable {

    final long cash

    final Map<YahooStockTicker, Integer> shares = [:]

    Portfolio(long cash, initialShares = [:]) {
        this.cash = cash
        this.shares.putAll(initialShares)
    }

    public Map<YahooStockTicker, Integer> getShares() {
        return Collections.unmodifiableMap(shares)
    }

    @Override
    public String toString() {
        def money = MathUtil.formatMoney(cash)
        StringBuilder sb = new StringBuilder("Cash: ").append(money).
                append('  Shares: ').append(shares.collect { return it.key.ticker + ': ' + it.value})
        return sb.toString()
    }
}
