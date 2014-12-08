package com.jdom.bodycomposition.domain.algorithm

/**
 * Created by djohnson on 12/7/14.
 */
class PortfolioValue {
    final Portfolio portfolio
    final Date date
    final Set<PositionValue> positions

    PortfolioValue(final Portfolio portfolio, final Date date, final Set<PositionValue> positions) {
        this.portfolio = portfolio
        this.date = date
        this.positions = positions
    }

    long marketValue() {
        long value = portfolio.cash
        for (PositionValue position : positions) {
            value += position.marketValue()
        }

        return value
    }

    long getCash() {
        return portfolio.cash
    }
}