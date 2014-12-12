package com.jdom.bodycomposition.domain.algorithm
import com.jdom.bodycomposition.domain.BaseSecurity
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
/**
 * Created by djohnson on 12/6/14.
 */
@ToString(includeSuper = true)
@EqualsAndHashCode(callSuper = true)
class BuyTransaction extends TransferTransaction {

    BuyTransaction(BaseSecurity security, Date date, int numberOfShares, long price, long commission) {
        super(security, date, numberOfShares, price, commission)
    }

    @Override
    protected Portfolio createNewPortfolio(Portfolio existing) {
        long newCash = existing.cash + cashValue - commission

        def positions = new HashSet<Position>()
        positions.addAll(existing.positions)

        def position = positions.find{ it.security == security }
        if (position == null) {
            position = new Position(security, numberOfShares)
        } else {
            position = new Position(security, position.shares + numberOfShares)
        }
        positions.add(position)

        return new Portfolio(newCash, existing.commissionCost, positions)
    }

    @Override
    public String getAction() {
        return "Buy"
    }

    @Override
    long getCashValue() {
        return -(price * numberOfShares)
    }

    @Override
    BuyTransaction forDate(final Date date) {
        return new BuyTransaction(security, date, numberOfShares, price, commission)
    }
}
