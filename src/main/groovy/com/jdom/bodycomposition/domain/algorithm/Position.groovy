package com.jdom.bodycomposition.domain.algorithm

import com.jdom.bodycomposition.domain.BaseSecurity
import com.jdom.bodycomposition.domain.Stock
import groovy.transform.ToString

/**
 * Created by djohnson on 12/7/14.
 */
@ToString
class Position {

    final BaseSecurity security

    final int shares

    Position(Stock security, int shares) {
        this.security = security
        this.shares = shares
    }

    boolean equals(final o) {
        if (this.is(o)) return true
        if (!(o instanceof Position)) return false

        final Position position = (Position) o

        if (security != position.security) return false

        return true
    }

    int hashCode() {
        return security.hashCode()
    }
}
