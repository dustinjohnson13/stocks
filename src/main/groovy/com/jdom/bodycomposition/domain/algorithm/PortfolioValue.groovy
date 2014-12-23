package com.jdom.bodycomposition.domain.algorithm

import com.jdom.util.MathUtil
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

/**
 * Created by djohnson on 12/7/14.
 */
@EqualsAndHashCode
@ToString
class PortfolioValue implements Serializable {
    final Portfolio portfolio
    final Date date
    final Set<PositionValue> positions

    private PortfolioValue(final Portfolio portfolio, final Date date, final Set<PositionValue> positions) {
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

    long percentChangeFrom(final PortfolioValue portfolio) {
        def currentValue = marketValue()
        def originalValue = portfolio.marketValue()

        return MathUtil.percentChange(currentValue, originalValue)
    }

    static PortfolioValue newPortfolioValue(final Portfolio portfolio, final Date date, final Set<PositionValue> positions) {
        return new PortfolioValue(portfolio, date, positions)
    }
}