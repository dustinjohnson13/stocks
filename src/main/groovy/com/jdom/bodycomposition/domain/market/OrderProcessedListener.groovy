package com.jdom.bodycomposition.domain.market

/**
 * Created by djohnson on 12/11/14.
 */
interface OrderProcessedListener {
    void orderFilled(OrderRequest order)

    void orderCancelled(OrderRequest order)
}