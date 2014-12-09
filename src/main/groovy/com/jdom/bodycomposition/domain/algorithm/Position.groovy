package com.jdom.bodycomposition.domain.algorithm

import com.jdom.bodycomposition.domain.BaseSecurity
import com.jdom.bodycomposition.domain.Stock
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

/**
 * Created by djohnson on 12/7/14.
 */
@ToString
@EqualsAndHashCode(includes = ['security'])
class Position implements Serializable {

    final BaseSecurity security

    final int shares

    Position(Stock security, int shares) {
        this.security = security
        this.shares = shares
    }
}
