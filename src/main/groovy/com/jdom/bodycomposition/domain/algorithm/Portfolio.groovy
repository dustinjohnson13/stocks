package com.jdom.bodycomposition.domain.algorithm

import com.jdom.util.MathUtil
import groovy.transform.EqualsAndHashCode

/**
 * Created by djohnson on 12/6/14.
 */
@EqualsAndHashCode
class Portfolio implements Serializable {

    final long cash

    final Set<Position> positions = new HashSet<>()

    Portfolio(long cash, Set<Position> initialShares = new HashSet<Position>()) {
        this.cash = cash
        this.positions.addAll(initialShares)
    }

    public Set<Position> getPositions() {
        return Collections.unmodifiableSet(positions)
    }

    @Override
    public String toString() {
        def money = MathUtil.formatMoney(cash)
        StringBuilder sb = new StringBuilder("Cash: ").append(money).
                append('  Shares: ').append(positions.collect { return it.security.symbol + ': ' + it.shares})
        return sb.toString()
    }
}
