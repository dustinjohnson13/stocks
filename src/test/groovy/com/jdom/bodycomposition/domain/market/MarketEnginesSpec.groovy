package com.jdom.bodycomposition.domain.market

import com.jdom.bodycomposition.domain.BaseSecurity
import com.jdom.bodycomposition.domain.market.orders.Duration
import com.jdom.bodycomposition.domain.market.orders.Orders
import com.jdom.bodycomposition.service.DailySecurityDataDao
import com.jdom.bodycomposition.service.SpringProfiles
import com.jdom.bodycomposition.service.StockDao
import com.jdom.bodycomposition.service.StocksServiceContext
import com.jdom.util.TimeUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.annotation.DirtiesContext
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
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class MarketEnginesSpec extends Specification {

    @Autowired
    StockDao securityDao
    @Autowired
    DailySecurityDataDao dailySecurityDataDao

    BaseSecurity fb

    BaseSecurity msft

    MarketEngine market

    OrderProcessedListener listener = Mock()

    def sundayNonMarketDay = dateFromDashString('2013-12-01')
    def mondayMarketDay = dateFromDashString('2013-12-02')

    def setup() {
        fb = securityDao.findBySymbol('FB')
        msft = securityDao.findBySymbol('MSFT')
        market = MarketEngines.create(dailySecurityDataDao)
        market.registerOrderFilledListener(listener)
        market.processDay(sundayNonMarketDay)
    }

    @Unroll
    def 'should fill open #type market order at open price'() {

        def order = ('buy' == type) ? Orders.newBuyMarketOrder(10, msft) : Orders.newSellMarketOrder(10, msft)

        given: 'a #type market order submitted at the end of the day'
        def submittedOrder = market.submit(order)

        and: 'the order was submitted successfully'
        submittedOrder.id != null
        submittedOrder.status == OrderStatus.OPEN

        when: 'the market processes the next day'
        market.processDay(mondayMarketDay)

        def processedOrder = market.getOrder(submittedOrder)
        then: 'the order is processed'
        processedOrder
        processedOrder.status == OrderStatus.EXECUTED

        and: 'the order was executed at open price'
        processedOrder.executionPrice == 3809

        and: 'the listener was notified'
        listener.orderFilled(processedOrder)

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
        market.processDay(mondayMarketDay)

        def processedOrder = market.getOrder(submittedOrder)
        then: 'the order is processed'
        processedOrder
        processedOrder.status == OrderStatus.EXECUTED

        and: 'the order was executed at the limit price'
        processedOrder.executionPrice == order.price

        and: 'the listener was notified'
        listener.orderFilled(processedOrder)

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
        market.processDay(mondayMarketDay)

        def processedOrder = market.getOrder(submittedOrder)
        then: 'the order is not processed'
        processedOrder
        processedOrder.status == expectedStatus

        and: 'the listener was notified if the order was cancelled'
        if (expectedStatus == OrderStatus.CANCELLED) {
            1 * listener.orderCancelled(_ as OrderRequest)
        }

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
    def 'should cancel open stop #type order after one year if the stop price is always above the low/high for the day'() {

        def order = ('buy' == type) ? Orders.newBuyStopOrder(10, msft, stopPrice, Duration.GTC) :
              Orders.newSellStopOrder(10, msft, stopPrice, Duration.GTC)

        given: 'a #type stop order submitted before day start with a price that will not be reached in the next year'
        def submittedOrder = market.submit(order)

        when: 'the order was submitted successfully'
        submittedOrder.id != null
        submittedOrder.status == OrderStatus.OPEN

        then: 'the order is left open for one year'
        364.times {
            sundayNonMarketDay = TimeUtil.oneDayLater(sundayNonMarketDay)
            market.processDay(sundayNonMarketDay)

            def processedOrder = market.getOrder(submittedOrder)
            assert processedOrder.status == OrderStatus.OPEN
        }

        then: 'the next day the order is cancelled'
        market.processDay(TimeUtil.oneDayLater(sundayNonMarketDay))
        def processedOrder = market.getOrder(submittedOrder)
        processedOrder.status == OrderStatus.CANCELLED

        then: 'the listener is notified'
        1 * listener.orderCancelled(_ as OrderRequest)

        where:
        type   | stopPrice
        'buy'  | 2
        'sell' | 10000
    }

    @Unroll
    def 'should not fill open #type stop order at its price if the price is above the low/high for the day'() {

        def order = ('buy' == type) ? Orders.newBuyStopOrder(10, msft, stopPrice, duration) :
              Orders.newSellStopOrder(10, msft, stopPrice, duration)

        given: 'a #type stop order submitted before day start'
        def submittedOrder = market.submit(order)

        and: 'the order was submitted successfully'
        submittedOrder.id != null
        submittedOrder.status == OrderStatus.OPEN

        when: 'the market processes the next day'
        market.processDay(mondayMarketDay)

        def processedOrder = market.getOrder(submittedOrder)
        then: 'the order is not processed'
        processedOrder
        processedOrder.status == expectedStatus

        and: 'the listener was notified if the order was cancelled'
        if (expectedStatus == OrderStatus.CANCELLED) {
            1 * listener.orderCancelled(_ as OrderRequest)
        }

        where:
        type   | stopPrice | duration           | expectedStatus
        'buy'  | 3805      | Duration.GTC       | OrderStatus.OPEN // 1 cent below low price
        'buy'  | 3706      | Duration.GTC       | OrderStatus.OPEN // 1 dollar below low price
        'buy'  | 3805      | Duration.DAY_ORDER | OrderStatus.CANCELLED // 1 cent below low price
        'buy'  | 3706      | Duration.DAY_ORDER | OrderStatus.CANCELLED // 1 dollar below low price
        'sell' | 3879      | Duration.GTC       | OrderStatus.OPEN // 1 cent above high price
        'sell' | 3978      | Duration.GTC       | OrderStatus.OPEN // 1 dollar above high price
        'sell' | 3879      | Duration.DAY_ORDER | OrderStatus.CANCELLED // 1 cent above high price
        'sell' | 3978      | Duration.DAY_ORDER | OrderStatus.CANCELLED // 1 dollar above high price
    }

    @Unroll
    def 'should cancel open #type stop order after one year if the stop price is always above the low/high for the day'() {

        def order = ('buy' == type) ? Orders.newBuyStopOrder(10, msft, stopPrice, Duration.GTC) :
              Orders.newSellStopOrder(10, msft, stopPrice, Duration.GTC)

        given: 'a #type stop order submitted before day start with a price that will not be reached in the next year'
        def submittedOrder = market.submit(order)

        when: 'the order was submitted successfully'
        submittedOrder.id != null
        submittedOrder.status == OrderStatus.OPEN

        then: 'the order is left open for one year'
        364.times {
            sundayNonMarketDay = TimeUtil.oneDayLater(sundayNonMarketDay)
            market.processDay(sundayNonMarketDay)

            def processedOrder = market.getOrder(submittedOrder)
            assert processedOrder.status == OrderStatus.OPEN
        }

        then: 'the next day the order is cancelled'
        market.processDay(TimeUtil.oneDayLater(sundayNonMarketDay))
        def processedOrder = market.getOrder(submittedOrder)
        processedOrder.status == OrderStatus.CANCELLED

        then: 'the listener is notified'
        1 * listener.orderCancelled(_ as OrderRequest)

        where:
        type   | stopPrice
        'buy'  | 2
        'sell' | 10000
    }

    @Unroll
    def 'should cancel open stop limit #type order after one year if the limit price is always above the low/high for the day'() {

        def order = ('buy' == type) ? Orders.newBuyStopLimitOrder(10, msft, stopPrice, limitPrice, Duration.GTC) :
              Orders.newSellStopLimitOrder(10, msft, stopPrice, limitPrice, Duration.GTC)

        given: 'a #type stop limit order submitted before day start with a price that will not be reached in the next year'
        def submittedOrder = market.submit(order)

        when: 'the order was submitted successfully'
        submittedOrder.id != null
        submittedOrder.status == OrderStatus.OPEN

        then: 'the order is left open for one year'
        364.times {
            sundayNonMarketDay = TimeUtil.oneDayLater(sundayNonMarketDay)
            market.processDay(sundayNonMarketDay)

            def processedOrder = market.getOrder(submittedOrder)
            assert processedOrder.status == OrderStatus.OPEN
        }

        then: 'the next day the order is cancelled'
        market.processDay(TimeUtil.oneDayLater(sundayNonMarketDay))
        def processedOrder = market.getOrder(submittedOrder)
        processedOrder.status == OrderStatus.CANCELLED

        then: 'the listener is notified'
        1 * listener.orderCancelled(_ as OrderRequest)

        where:
        type   | stopPrice | limitPrice
        'buy'  | 3         | 2
        'sell' | 10001     | 10000
    }

    @Unroll
    def 'should not fill open #type stop limit order at its limit price if the stop price is hit but the limit price is above/below the low/high for the day'() {

        def order = ('buy' == type) ? Orders.newBuyStopLimitOrder(10, msft, stopPrice, limitPrice, duration) :
              Orders.newSellStopLimitOrder(10, msft, stopPrice, limitPrice, duration)

        given: 'a #type stop limit order submitted before day start'
        def submittedOrder = market.submit(order)

        and: 'the order was submitted successfully'
        submittedOrder.id != null
        submittedOrder.status == OrderStatus.OPEN

        when: 'the market processes the next day'
        market.processDay(mondayMarketDay)

        def processedOrder = market.getOrder(submittedOrder)
        then: 'the order is not processed'
        processedOrder
        processedOrder.status == expectedStatus

        and: 'the listener was notified if the order was cancelled'
        if (expectedStatus == OrderStatus.CANCELLED) {
            1 * listener.orderCancelled(_ as OrderRequest)
        }

        where:
        type   | stopPrice | limitPrice | duration           | expectedStatus
        'buy'  | 3806      | 3805       | Duration.GTC       | OrderStatus.OPEN // 1 cent below low price
        'buy'  | 3806      | 3706       | Duration.GTC       | OrderStatus.OPEN // 1 dollar below low price
        'buy'  | 3806      | 3805       | Duration.DAY_ORDER | OrderStatus.CANCELLED // 1 cent below low price
        'buy'  | 3806      | 3706       | Duration.DAY_ORDER | OrderStatus.CANCELLED // 1 dollar below low price
        'sell' | 3878      | 3879       | Duration.GTC       | OrderStatus.OPEN // 1 cent above high price
        'sell' | 3868      | 3978       | Duration.GTC       | OrderStatus.OPEN // 1 dollar above high price
        'sell' | 3878      | 3879       | Duration.DAY_ORDER | OrderStatus.CANCELLED // 1 cent above high price
        'sell' | 4078      | 3978       | Duration.DAY_ORDER | OrderStatus.CANCELLED // 1 dollar above high price
    }

    @Unroll
    def 'should cancel open #type stop limit order after one year if the limit price is always above the low/high for the day'() {

        def order = ('buy' == type) ? Orders.newBuyStopLimitOrder(10, msft, stopPrice, limitPrice, Duration.GTC) :
              Orders.newSellStopLimitOrder(10, msft, stopPrice, limitPrice, Duration.GTC)

        given: 'a #type stop order submitted before day start with a price that will not be reached in the next year'
        def submittedOrder = market.submit(order)

        when: 'the order was submitted successfully'
        submittedOrder.id != null
        submittedOrder.status == OrderStatus.OPEN

        then: 'the order is left open for one year'
        364.times {
            sundayNonMarketDay = TimeUtil.oneDayLater(sundayNonMarketDay)
            market.processDay(sundayNonMarketDay)

            def processedOrder = market.getOrder(submittedOrder)
            assert processedOrder.status == OrderStatus.OPEN
        }

        then: 'the next day the order is cancelled'
        market.processDay(TimeUtil.oneDayLater(sundayNonMarketDay))
        def processedOrder = market.getOrder(submittedOrder)
        processedOrder.status == OrderStatus.CANCELLED

        then: 'the listener is notified'
        1 * listener.orderCancelled(_ as OrderRequest)

        where:
        type   | stopPrice | limitPrice
        'buy'  | 3800      | 2
        'sell' | 3800      | 10000
    }

    def 'should consider order status listener registration idempotent, order cancelling'() {

        def order = Orders.newBuyLimitOrder(10, msft, 1L, Duration.DAY_ORDER)

        given: 'the same listener is registered more than once'
        market.registerOrderFilledListener(listener)
        market.registerOrderFilledListener(listener)

        and: 'a limit order that will not be executed is submitted'
        market.submit(order)

        when: 'an order is cancelled'
        market.processDay(TimeUtil.oneDayLater(sundayNonMarketDay))

        then: 'the listener will only be notified once'
        1 * listener.orderCancelled(_ as OrderRequest)
    }

    def 'should consider order status listener registration idempotent, order filling'() {

        def order = Orders.newBuyLimitOrder(10, msft, 1000000L, Duration.DAY_ORDER)

        given: 'the same listener is registered more than once'
        market.registerOrderFilledListener(listener)
        market.registerOrderFilledListener(listener)

        and: 'a limit order that will be executed is submitted'
        market.submit(order)

        when: 'an order is filled'
        market.processDay(TimeUtil.oneDayLater(sundayNonMarketDay))

        then: 'the listener will only be notified once'
        1 * listener.orderFilled(_ as OrderRequest)
    }

    def 'should throw exception when trying to register a null listener'() {
        when: 'a null listener is registered'
        market.registerOrderFilledListener(null)

        then: 'an exception is thrown'
        thrown IllegalArgumentException
    }

    def 'should be able to submit multiple orders the same date'() {

        def firstOrder = Orders.newBuyLimitOrder(10, msft, 1000000L, Duration.DAY_ORDER)
        def secondOrder = Orders.newBuyLimitOrder(10, fb, 1000000L, Duration.DAY_ORDER)

        given: 'one order is submitted'
        def firstOrderRequest = market.submit(firstOrder)

        and: 'another order is submitted the same day for a different security'
        def secondOrderRequest = market.submit(secondOrder)

        when: 'the market day is processed'
        market.processDay(mondayMarketDay)

        then: 'both orders should have been filled'
        1 * listener.orderFilled(firstOrderRequest)
        1 * listener.orderFilled(secondOrderRequest)
    }

}
