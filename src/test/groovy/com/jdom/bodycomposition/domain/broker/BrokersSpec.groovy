package com.jdom.bodycomposition.domain.broker

import com.jdom.bodycomposition.domain.BaseSecurity
import com.jdom.bodycomposition.domain.algorithm.Portfolio
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
    BaseSecurity security = Mock()

    Portfolio portfolio = new Portfolio(toMoney('$200'))

    Broker broker = Brokers.create(market, portfolio, toMoney('$5'))

    def setup() {

        _ * mockOrderRequest.id >> 'orderId'

        _ * market.submit(_ as Order) >> mockOrderRequest
    }

    def 'should submit order through to market'() {

        given: 'a portfolio with $200 and a commission of $5'
        assert portfolio.cash == 20000L
        assert broker.commissionCost == 500L

        and: 'an order with a cost of $190 not including commission'
        def order = Orders.newBuyLimitOrder(10, security, 19, Duration.DAY_ORDER)

        when: 'the order is submitted'
        broker.submit(order)

        then: 'the order is submitted to the market'
        1 * market.submit(order) >> mockOrderRequest
    }

    def 'should pass request for order status through to market'() {

        Portfolio portfolio = new Portfolio(toMoney('$200'))
        Broker broker = Brokers.create(market, portfolio, toMoney('$5'))
        OrderRequest order = Mock()

        when: 'an order status is submitted'
        def status = broker.getOrder(order)

        then: 'the order status is passed through to the market'
        1 * market.getOrder(order) >> order

        and: 'the result is returned to the user'
        status.is(order)
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
        Portfolio portfolio = new Portfolio(toMoney('$200'))
        Broker broker = Brokers.create(market, portfolio, toMoney('$5'))

        when: 'an invalid order is submitted'
        def processedOrder = broker.submit(order)

        then: 'the order was rejected'
        thrown OrderRejectedException

        where:
        reason             | order
        'shares not owned' | Orders.newSellLimitOrder(10, security, toMoney('$19'), Duration.DAY_ORDER)
        'not enough money' | Orders.newBuyLimitOrder(10, security, toMoney('$20'), Duration.DAY_ORDER)
    }
}
