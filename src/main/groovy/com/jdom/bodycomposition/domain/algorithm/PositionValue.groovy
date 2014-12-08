package com.jdom.bodycomposition.domain.algorithm
import com.jdom.bodycomposition.domain.BaseSecurity
import groovy.transform.EqualsAndHashCode
/**
 * Created by djohnson on 12/7/14.
 */
@EqualsAndHashCode(includes = 'position')
class PositionValue {
    final Position position
    final Date date
    final long price

    PositionValue(Position position, Date date, long price) {
        this.position = position
        this.date = date
        this.price = price
    }

    long marketValue() {
        return price * position.shares
    }

    BaseSecurity getSecurity() {
        return position.security
    }

    int getShares() {
        return position.shares
    }
}
