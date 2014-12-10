package com.jdom.bodycomposition.domain.market.orders

import com.jdom.bodycomposition.domain.Stock
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Created by djohnson on 12/10/14.
 */
class OrdersSpec extends Specification {

    @Shared
    def security = new Stock(id: 1L, symbol: 'MSFT', exchange: 'NYSE')

    @Unroll
    def 'should be able to create #type market orders'() {

        expect: 'it has the correct attributes'
        expectedType.isAssignableFrom(order.class)
        order.shares == 10
        order.security == security

        where:
        type   | order                                   | expectedType
        'buy'  | Orders.newBuyMarketOrder(10, security)  | BuyMarketOrder
        'sell' | Orders.newSellMarketOrder(10, security) | SellMarketOrder
    }

    @Unroll
    def 'should be able to create #type limit orders'() {

        expect: 'it has the correct attributes'
        expectedType.isAssignableFrom(order.class)
        order.shares == 10
        order.security == security
        order.price == 2550L
        order.duration == Duration.GTC

        where:
        type   | order                                                       | expectedType
        'buy'  | Orders.newBuyLimitOrder(10, security, 2550L, Duration.GTC)  | BuyLimitOrder
        'sell' | Orders.newSellLimitOrder(10, security, 2550L, Duration.GTC) | SellLimitOrder
    }

    @Unroll
    def 'should be able to create #type stop orders'() {

        expect: 'it has the correct attributes'
        expectedType.isAssignableFrom(order.class)
        order.shares == 10
        order.security == security
        order.stopPrice == 2550L
        order.duration == Duration.GTC

        where:
        type   | order                                                      | expectedType
        'buy'  | Orders.newBuyStopOrder(10, security, 2550L, Duration.GTC)  | BuyStopOrder
        'sell' | Orders.newSellStopOrder(10, security, 2550L, Duration.GTC) | SellStopOrder
    }

    @Unroll
    def 'should be able to create #type stop limit orders'() {

        expect: 'it has the correct attributes'
        expectedType.isAssignableFrom(order.class)
        order.shares == 10
        order.security == security
        order.stopPrice == 2550L
        order.price == 2950L
        order.duration == Duration.GTC

        where:
        type   | order                                                                  | expectedType
        'buy'  | Orders.newBuyStopLimitOrder(10, security, 2550L, 2950L, Duration.GTC)  | BuyStopLimitOrder
        'sell' | Orders.newSellStopLimitOrder(10, security, 2550L, 2950L, Duration.GTC) | SellStopLimitOrder
    }

    @Unroll
    def 'should be able to create #type market on close orders'() {

        expect: 'it has the correct attributes'
        expectedType.isAssignableFrom(order.class)
        order.shares == 10
        order.security == security

        where:
        type   | order                                          | expectedType
        'buy'  | Orders.newBuyMarketOnCloseOrder(10, security)  | BuyMarketOnCloseOrder
        'sell' | Orders.newSellMarketOnCloseOrder(10, security) | SellMarketOnCloseOrder
    }
}
