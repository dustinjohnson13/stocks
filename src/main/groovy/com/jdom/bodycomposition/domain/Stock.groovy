package com.jdom.bodycomposition.domain
import groovy.transform.EqualsAndHashCode

import javax.persistence.Entity
import javax.persistence.Table

/**
 * Created by djohnson on 11/14/14.
 */
@Entity
@Table(name = 'yahoo_stock_ticker')
@EqualsAndHashCode
class Stock extends BaseSecurity {
}
