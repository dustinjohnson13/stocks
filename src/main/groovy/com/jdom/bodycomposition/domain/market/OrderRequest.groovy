package com.jdom.bodycomposition.domain.market

import com.jdom.bodycomposition.domain.market.orders.Order

/**
 * Created by djohnson on 12/10/14.
 */
interface OrderRequest extends Order {
    String getId()
    Date getSubmissionDate()
    OrderStatus getStatus()
    long getExecutionPrice()
}