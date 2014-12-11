package com.jdom.bodycomposition.domain.market

import com.jdom.bodycomposition.domain.BaseSecurity
import com.jdom.bodycomposition.domain.market.orders.Duration
import com.jdom.bodycomposition.domain.market.orders.Orders
import com.jdom.bodycomposition.service.DailySecurityDataDao
import com.jdom.bodycomposition.service.SpringProfiles
import com.jdom.bodycomposition.service.StockDao
import com.jdom.bodycomposition.service.StocksServiceContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.transaction.TransactionConfiguration
import spock.lang.Specification
import spock.lang.Unroll

import javax.transaction.Transactional

import static com.jdom.util.TimeUtil.dateFromDashString

/**
 * Created by djohnson on 12/10/14.
 */
@ActiveProfiles(SpringProfiles.TEST)
@ContextConfiguration(classes = [StocksServiceContext.class])
@Transactional
@TransactionConfiguration(defaultRollback = true)
class MarketEnginesSpec extends Specification {

    @Autowired
    StockDao securityDao
    @Autowired
    DailySecurityDataDao dailySecurityDataDao

    BaseSecurity msft

    MarketEngine market

    def setup() {
        msft = securityDao.findBySymbol('MSFT')
        market = MarketEngines.create(dateFromDashString('2013-12-02'),
                dateFromDashString('2013-12-05'), dailySecurityDataDao)
    }

    @Unroll
    def 'should fill open #type market order at open price'() {

        def order = ('buy' == type) ? Orders.newBuyMarketOrder(10, msft) : Orders.newSellMarketOrder(10, msft)

        given: 'a #type market order submitted before day start'
        def submittedOrder = market.submit(order)

        and: 'the order was submitted successfully'
        submittedOrder.id != null
        submittedOrder.status == OrderStatus.OPEN

        when: 'the market processes the next day'
        market.processDay()

        def processedOrder = market.getOrder(submittedOrder)
        then: 'the order is processed'
        processedOrder
        processedOrder.status == OrderStatus.EXECUTED

        and: 'the order was executed at open price'
        processedOrder.executionPrice == 3809

        where:
        type << ['buy', 'sell']
    }

    @Unroll
    def 'should fill open #type limit order at its price if the price is within the high and low for the day'() {

        def order = ('buy' == type) ? Orders.newBuyLimitOrder(10, msft, limitPrice, duration) :
                Orders.newSellLimitOrder(10, msft, limitPrice, duration)

        given: 'a #type limit order submitted before day start'
        def submittedOrder = market.submit(order)

        and: 'the order was submitted successfully'
        submittedOrder.id != null
        submittedOrder.status == OrderStatus.OPEN

        when: 'the market processes the next day'
        market.processDay()

        def processedOrder = market.getOrder(submittedOrder)
        then: 'the order is processed'
        processedOrder
        processedOrder.status == OrderStatus.EXECUTED

        and: 'the order was executed at the limit price'
        processedOrder.executionPrice == order.price

        where:
        type   | limitPrice | duration
        'buy'  | 3806       | Duration.GTC // Low price exactly
        'buy'  | 3841       | Duration.GTC // In-between price
        'buy'  | 3878       | Duration.GTC // High price exactly
        'sell' | 3806       | Duration.GTC // Low price exactly
        'sell' | 3841       | Duration.GTC // In-between price
        'sell' | 3878       | Duration.GTC // High price exactly
        'buy'  | 3806       | Duration.DAY_ORDER // Low price exactly
        'buy'  | 3841       | Duration.DAY_ORDER // In-between price
        'buy'  | 3878       | Duration.DAY_ORDER // High price exactly
        'sell' | 3806       | Duration.DAY_ORDER // Low price exactly
        'sell' | 3841       | Duration.DAY_ORDER // In-between price
        'sell' | 3878       | Duration.DAY_ORDER // High price exactly
    }

    @Unroll
    def 'should not fill open #type limit order at its price if the price is above the low/high for the day'() {

        def order = ('buy' == type) ? Orders.newBuyLimitOrder(10, msft, limitPrice, duration) :
                Orders.newSellLimitOrder(10, msft, limitPrice, duration)

        given: 'a #type limit order submitted before day start'
        def submittedOrder = market.submit(order)

        and: 'the order was submitted successfully'
        submittedOrder.id != null
        submittedOrder.status == OrderStatus.OPEN

        when: 'the market processes the next day'
        market.processDay()

        def processedOrder = market.getOrder(submittedOrder)
        then: 'the order is not processed'
        processedOrder
        processedOrder.status == expectedStatus

        where:
        type   | limitPrice | duration           | expectedStatus
        'buy'  | 3805       | Duration.GTC       | OrderStatus.OPEN // 1 cent below low price
        'buy'  | 3706       | Duration.GTC       | OrderStatus.OPEN // 1 dollar below low price
        'buy'  | 3805       | Duration.DAY_ORDER | OrderStatus.CANCELLED // 1 cent below low price
        'buy'  | 3706       | Duration.DAY_ORDER | OrderStatus.CANCELLED // 1 dollar below low price
        'sell' | 3879       | Duration.GTC       | OrderStatus.OPEN // 1 cent above high price
        'sell' | 3978       | Duration.GTC       | OrderStatus.OPEN // 1 dollar above high price
        'sell' | 3879       | Duration.DAY_ORDER | OrderStatus.CANCELLED // 1 cent above high price
        'sell' | 3978       | Duration.DAY_ORDER | OrderStatus.CANCELLED // 1 dollar above high price
    }

    @Unroll
    def 'should cancel open #type limit order after one year if the limit price is always above the low/high for the day'() {

        def order = ('buy' == type) ? Orders.newBuyLimitOrder(10, msft, limitPrice, Duration.GTC) :
                Orders.newSellLimitOrder(10, msft, limitPrice, Duration.GTC)

        given: 'a #type limit order submitted before day start with a price that will not be reached in the next year'
        def submittedOrder = market.submit(order)

        when: 'the order was submitted successfully'
        submittedOrder.id != null
        submittedOrder.status == OrderStatus.OPEN

        then: 'the order is left open for one year'
        365.times {
            market.processDay()

            def processedOrder = market.getOrder(submittedOrder)
            assert processedOrder.status == OrderStatus.OPEN
        }

        and: 'the next day the order is cancelled'
        market.processDay()
        def processedOrder = market.getOrder(submittedOrder)
        processedOrder.status == OrderStatus.CANCELLED

        where:
        type   | limitPrice
        'buy'  | 2
        'sell' | 10000
    }
}
