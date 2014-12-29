package com.jdom.bodycomposition.domain.market.orders

/**
 * Created by djohnson on 12/29/14.
 */
interface OneCancelsOther {
    Order firstOrder
    Order secondOrder
}