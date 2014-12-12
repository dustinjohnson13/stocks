package com.jdom.bodycomposition.domain.market.orders;

/**
 * Created by djohnson on 12/11/14.
 */
public class OrderRejectedException extends Exception {
    public OrderRejectedException(Exception e) {
        super(e);
    }
}
