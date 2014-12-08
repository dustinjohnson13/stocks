package com.jdom.bodycomposition.domain.algorithm

import com.jdom.util.MathUtil
/**
 * Created by djohnson on 12/6/14.
 */
class Portfolio implements Serializable {

    final long cash

    final Set<Position> positions = new HashSet<>()

    final long commissionCost

    Portfolio(long cash, long commissionCost, Set<Position> initialShares = new HashSet<Position>()) {
        this.cash = cash
        this.commissionCost = commissionCost
        this.positions.addAll(initialShares)
    }

    public Set<Position> getPositions() {
        return Collections.unmodifiableSet(positions)
    }

    @Override
    public String toString() {
        def money = MathUtil.formatMoney(cash)
        StringBuilder sb = new StringBuilder("Cash: ").append(money).append('  Commission: ').append(MathUtil.formatMoney(commissionCost)).
                append('  Shares: ').append(positions.collect { return it.security.symbol + ': ' + it.shares})
        return sb.toString()
    }
}
