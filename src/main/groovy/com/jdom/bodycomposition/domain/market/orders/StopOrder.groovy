package com.jdom.bodycomposition.domain.market.orders

/**
 * Created by djohnson on 12/10/14.
 */
interface StopOrder extends Order {
    Duration getDuration()
    long getStopPrice()
}