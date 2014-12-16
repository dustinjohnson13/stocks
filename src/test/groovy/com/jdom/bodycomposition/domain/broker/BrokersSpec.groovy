package com.jdom.bodycomposition.domain.broker

import com.jdom.bodycomposition.domain.BaseSecurity
import com.jdom.bodycomposition.domain.Stock
import com.jdom.bodycomposition.domain.algorithm.Portfolio
import com.jdom.bodycomposition.domain.algorithm.Position
import com.jdom.bodycomposition.domain.market.Market
import com.jdom.bodycomposition.domain.market.OrderRequest
import com.jdom.bodycomposition.domain.market.orders.Duration
import com.jdom.bodycomposition.domain.market.orders.Order
import com.jdom.bodycomposition.domain.market.orders.OrderRejectedException
import com.jdom.bodycomposition.domain.market.orders.Orders
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import static com.jdom.util.MathUtil.toMoney

/**
 * Created by djohnson on 12/11/14.
 */
class BrokersSpec extends Specification {

    Market market = Mock()

    OrderRequest mockOrderRequest = Mock()

    @Shared
    BaseSecurity msft = new Stock(id: 1L, symbol: 'MSFT', exchange: 'NYSE')

    @Shared
    BaseSecurity fb = new Stock(id: 1L, symbol: 'FB', exchange: 'NYSE')

    Portfolio portfolio = new Portfolio(toMoney('$200'))

    Broker broker = Brokers.create(market, portfolio, toMoney('$5'))

    def setup() {

        _ * mockOrderRequest.id >> 'orderId'

        _ * market.submit(_ as Broker, _ as Order) >> mockOrderRequest
    }

    def 'should submit order through to market'() {

        given: 'a portfolio with $200 and a commission of $5'
        assert portfolio.cash == 20000L
        assert broker.commissionCost == 500L

        and: 'an order with a cost of $190 not including commission'
        def order = Orders.newBuyLimitOrder(10, msft, 19, Duration.DAY_ORDER)

        when: 'the order is submitted'
        broker.submit(order)

        then: 'the order is submitted to the market'
        1 * market.submit(broker, order) >> mockOrderRequest
    }

    def 'should pass request for order status through to market'() {

        when: 'an order status is submitted'
        def status = broker.getOrder(mockOrderRequest)

        then: 'the order status is passed through to the market'
        1 * market.getOrder(mockOrderRequest) >> mockOrderRequest

        and: 'the result is returned to the user'
        status.is(mockOrderRequest)
    }

    def 'should be able to retrieve the portfolio'() {

        when: 'the portfolio is requested'
        def returnedPortfolio = broker.portfolio

        then: 'the portfolio is returned'
        assert returnedPortfolio == portfolio
    }

    def 'should be able to retrieve the commission cost'() {

        when: 'the commission cost is requested'
        def commission = broker.commissionCost

        then: 'the commission cost is returned'
        commission == 500L
    }

    @Unroll
    def 'should not be able to submit invalid orders: #reason'() {

        when: 'an invalid order is submitted'
        def processedOrder = broker.submit(order)

        then: 'the order was rejected'
        thrown OrderRejectedException

        where:
        reason             | order
        'shares not owned' | Orders.newSellLimitOrder(10, msft, toMoney('$19'), Duration.DAY_ORDER)
        'not enough money' | Orders.newBuyLimitOrder(10, msft, toMoney('$20'), Duration.DAY_ORDER)
    }

    @Unroll
    def 'should not be able to submit multiple pending orders that would result in #reason'() {
        Portfolio portfolio = new Portfolio(toMoney('$200'), [new Position(msft, 10)] as Set)
        Broker broker = Brokers.create(market, portfolio, toMoney('$5'))

        given: 'there are pending orders'
        broker.submit(firstOrder)

        when: 'another order is submitted which would result in an invalid state if the first order is filled'
        broker.submit(secondOrder)

        then: 'the order is rejected'
        thrown OrderRejectedException

        where:
        reason                                                         | firstOrder                                                               | secondOrder
        'selling shares that will not exist'                           | Orders.newSellLimitOrder(10, msft, toMoney('$19'), Duration.DAY_ORDER)   | Orders.newSellLimitOrder(1, msft, toMoney('$19'), Duration.DAY_ORDER)
        'spending money that will not exist'                           | Orders.newBuyLimitOrder(10, msft, toMoney('$19'), Duration.DAY_ORDER)    | Orders.newBuyLimitOrder(1, msft, toMoney('$19'), Duration.DAY_ORDER)
        'spending money that may not exist if sell orders do not fill' | Orders.newSellLimitOrder(10, msft, toMoney('$19'), Duration.DAY_ORDER)   | Orders.newBuyLimitOrder(20, fb, toMoney('$19'), Duration.DAY_ORDER)
        'selling shares that may not exist if buy orders do not fill'  | Orders.newBuyLimitOrder(10, msft, toMoney('$19'), Duration.DAY_ORDER)    | Orders.newSellLimitOrder(20, msft, toMoney('$19'), Duration.DAY_ORDER)
        'not enough cash to pay the commission if all orders fill'     | Orders.newBuyLimitOrder(10, msft, toMoney('$19.50'), Duration.DAY_ORDER) | Orders.newSellLimitOrder(1, msft, toMoney('$4'), Duration.DAY_ORDER)
    }
}
