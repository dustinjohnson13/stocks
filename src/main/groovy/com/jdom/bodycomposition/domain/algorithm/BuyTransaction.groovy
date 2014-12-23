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
        positions.addAll(existing.positions.findAll{ it.security != security})

        def position = existing.positions.find{ it.security == security }
        if (position == null) {
            position = Position.newPosition(security, numberOfShares)
        } else {
            position = Position.newPosition(security, position.shares + numberOfShares)
        }
        if (position.shares != 0) {
            positions.add(position)
        }

        return Portfolio.newPortfolio(newCash, positions)
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
