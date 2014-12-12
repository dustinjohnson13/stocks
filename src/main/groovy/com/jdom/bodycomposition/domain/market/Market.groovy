package com.jdom.bodycomposition.domain.market

import com.jdom.bodycomposition.domain.market.orders.Order
/**
 * Created by djohnson on 12/10/14.
 */
interface Market {

    OrderRequest submit(Order marketOrder)

    OrderRequest getOrder(OrderRequest orderRequest)

    void registerOrderFilledListener(OrderProcessedListener listener)
}
