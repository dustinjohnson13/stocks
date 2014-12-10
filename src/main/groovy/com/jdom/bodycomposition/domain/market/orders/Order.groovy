package com.jdom.bodycomposition.domain.market.orders

import com.jdom.bodycomposition.domain.BaseSecurity

/**
 * Created by djohnson on 12/10/14.
 */
interface Order {
    int getShares()
    BaseSecurity getSecurity()
}