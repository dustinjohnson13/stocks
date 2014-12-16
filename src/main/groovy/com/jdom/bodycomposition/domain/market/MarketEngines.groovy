package com.jdom.bodycomposition.domain.market
import com.jdom.bodycomposition.domain.BaseSecurity
import com.jdom.bodycomposition.domain.DailySecurityData
import com.jdom.bodycomposition.domain.market.orders.BuyLimitOrder
import com.jdom.bodycomposition.domain.market.orders.BuyStopLimitOrder
import com.jdom.bodycomposition.domain.market.orders.BuyStopOrder
import com.jdom.bodycomposition.domain.market.orders.Duration
import com.jdom.bodycomposition.domain.market.orders.LimitOrder
import com.jdom.bodycomposition.domain.market.orders.MarketOrder
import com.jdom.bodycomposition.domain.market.orders.Order
import com.jdom.bodycomposition.domain.market.orders.SellLimitOrder
import com.jdom.bodycomposition.domain.market.orders.SellStopLimitOrder
import com.jdom.bodycomposition.domain.market.orders.SellStopOrder
import com.jdom.bodycomposition.domain.market.orders.StopLimitOrder
import com.jdom.bodycomposition.domain.market.orders.StopOrder
import com.jdom.bodycomposition.service.DailySecurityDataDao
import com.jdom.util.TimeUtil
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.slf4j.Logger
import org.slf4j.LoggerFactory
/**
 * Created by djohnson on 12/10/14.
 */
final class MarketEngines {

    private static final Logger log = LoggerFactory.getLogger(MarketEngines)

    private MarketEngines() {
    }

    static MarketEngine create(final DailySecurityDataDao dailySecurityDataDao) {
        return new DailyDataDrivenMarketEngine(dailySecurityDataDao)
    }

    private static class DailyDataDrivenMarketEngine implements MarketEngine {
        private final TreeSet<OrderRequestImpl> openOrders = []
        private final TreeSet<OrderRequestImpl> processedOrders = []
        private final List<OrderProcessedListener> listeners = []
        final DailySecurityDataDao dailySecurityDataDao
        Date currentDate

        private DailyDataDrivenMarketEngine(final DailySecurityDataDao dailySecurityDataDao) {
            this.dailySecurityDataDao = dailySecurityDataDao
        }

        @Override
        OrderRequest submit(final Order marketOrder) {
            if (log.isDebugEnabled()) {
                log.debug "Order [$marketOrder] was received."
            }

            def orderRequest = new OrderRequestImpl(marketOrder, currentDate)
            openOrders.add(orderRequest)

            if (log.isDebugEnabled()) {
                log.debug "Open orders:\n${openOrders}"
            }

            return orderRequest
        }

        @Override
        void processDay(Date date) {
            currentDate = date

            if (log.isInfoEnabled()) {
                log.info "Market day [${date}] is being processed.  Open orders:\n${openOrders}"
            }

            Set<OrderRequestImpl> origList = new HashSet<>(openOrders)
            openOrders.clear()

            boolean anySecuritiesProcessed = false
            for (OrderRequestImpl order : origList) {
                if (log.isDebugEnabled()) {
                    log.debug "Order [${order.id}] is being processed."
                }
                switch (order.status) {
                    case OrderStatus.EXECUTED:
                    case OrderStatus.CANCELLED:
                    case OrderStatus.REJECTED:
                        throw new IllegalStateException("Found processed order ${order.id} in the open orders set!  This indicates a programming error!")
                    default:
                        break;
                }

                def securityData = dailySecurityDataDao.findBySecurityAndDate(order.security, date)
                if (securityData == null) {
                    if (log.isDebugEnabled()) {
                        log.debug "No security data to process order [${order.id}]."
                    }
                    continue;
                } else {
                    anySecuritiesProcessed = true
                }

                def processedOrder = order
                switch (order.order) {
                    case StopLimitOrder:
                        if (log.isDebugEnabled()) {
                            log.debug "Processing stop limit order [${order.id}]."
                        }
                        processedOrder = processStopLimitOrder(order, order.order, securityData)
                        break;
                    case LimitOrder:
                        if (log.isDebugEnabled()) {
                            log.debug "Processing limit order [${order.id}]."
                        }
                        processedOrder = processLimitOrder(order, order.order, securityData)
                        break;
                    case StopOrder:
                        if (log.isDebugEnabled()) {
                            log.debug "Processing stop order [${order.id}]."
                        }
                        processedOrder = processStopOrder(order, order.order, securityData)
                        break;
                    case MarketOrder:
                        if (log.isDebugEnabled()) {
                            log.debug "Processing market order [${order.id}]."
                        }
                        MarketOrder marketOrder = (MarketOrder) order.order
                        processedOrder = new OrderRequestImpl(order, OrderStatus.EXECUTED, currentDate, securityData.open)
                        break;
                }

                if (log.isDebugEnabled()) {
                    log.debug "Order [${processedOrder.id}] has status [${processedOrder.status}]."
                }

                if (processedOrder.status == OrderStatus.EXECUTED || processedOrder.status == OrderStatus.CANCELLED) {
                    if (log.isDebugEnabled()) {
                        log.debug "Added [${processedOrder.id}] to processed orders."
                    }
                    processedOrders.add(processedOrder)
                    notifyListeners(processedOrder)
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug "Added [${processedOrder.id}] back to open orders."
                    }
                    openOrders.add(processedOrder)
                }
            }

            // Must be non-market day, restore orders list
            if (!anySecuritiesProcessed) {
                if (log.isDebugEnabled()) {
                    log.debug "No securities processed, adding all orders to open orders."
                }
                openOrders.addAll(origList)
            }

            if (log.isDebugEnabled()) {
                log.debug "Market day [${date}] is finished.  Open orders:\n${openOrders}"
            }
        }

        @Override
        void registerOrderFilledListener(final OrderProcessedListener listener) {
            if (listener == null) {
                throw new IllegalArgumentException("Cannot register a null OrderFilledListener!")
            }
            if (!listeners.contains(listener)) {
                listeners.add(listener)
            }
        }

        void notifyListeners(OrderRequest processedOrder) {
            if (log.isDebugEnabled()) {
                log.debug "Notifying ${listeners.size()} listeners of processed order [${processedOrder.id}]."
            }
            listeners.each {
                if (processedOrder.status == OrderStatus.EXECUTED) {
                    if (log.isDebugEnabled()) {
                        log.debug "Listener notified that order [${processedOrder.id}] was filled."
                    }
                    it.orderFilled(processedOrder)
                } else if (processedOrder.status == OrderStatus.CANCELLED) {
                    if (log.isDebugEnabled()) {
                        log.debug "Listener notified that order [${processedOrder.id}] was cancelled."
                    }
                    it.orderCancelled(processedOrder)
                } else {
                    throw new IllegalArgumentException("Unknown OrderStatus ${processedOrder.status} for notifying listeners!")
                }
            }
        }

        @Override
        OrderRequest getOrder(final OrderRequest orderRequest) {
            def order = openOrders.find { it.id == orderRequest.id }
            if (order == null) {
                order = processedOrders.find { it.id == orderRequest.id }
            }
            return order
        }

        OrderRequestImpl processStopLimitOrder(OrderRequestImpl order, BuyStopLimitOrder limitOrder, DailySecurityData securityData) {
            processLimitOrder(order, limitOrder) {
                limitOrder.price < securityData.low
            }
        }

        OrderRequestImpl processStopLimitOrder(OrderRequestImpl order, SellStopLimitOrder limitOrder, DailySecurityData securityData) {
            processLimitOrder(order, limitOrder) {
                limitOrder.price > securityData.high
            }
        }

        OrderRequestImpl processLimitOrder(OrderRequestImpl order, BuyLimitOrder limitOrder, DailySecurityData securityData) {
            processLimitOrder(order, limitOrder) {
                limitOrder.price < securityData.low
            }
        }

        OrderRequestImpl processLimitOrder(OrderRequestImpl order, SellLimitOrder limitOrder, DailySecurityData securityData) {
            processLimitOrder(order, limitOrder) {
                limitOrder.price > securityData.high
            }
        }

        OrderRequestImpl processStopOrder(OrderRequestImpl order, BuyStopOrder stopOrder, DailySecurityData securityData) {
            processStopOrder(order, stopOrder) {
                stopOrder.stopPrice < securityData.low
            }
        }

        OrderRequestImpl processStopOrder(OrderRequestImpl order, SellStopOrder stopOrder, DailySecurityData securityData) {
            processStopOrder(order, stopOrder) {
                stopOrder.stopPrice > securityData.high
            }
        }

        OrderRequestImpl processLimitOrder(OrderRequestImpl order, LimitOrder limitOrder, Closure<Boolean> unfillable) {
            if (unfillable.call()) {
                if (log.isDebugEnabled()) {
                    log.debug "Order [${order.id}] cannot be filled."
                }
                return checkForCancellation(order)
            }
            return executeOrder(order, limitOrder.price)
        }

        OrderRequestImpl processStopOrder(OrderRequestImpl order, StopOrder stopOrder, Closure<Boolean> unfillable) {
            if (unfillable.call()) {
                if (log.isDebugEnabled()) {
                    log.debug "Order [${order.id}] cannot be filled."
                }
                return checkForCancellation(order)
            }
            return executeOrder(order, stopOrder.stopPrice)
        }

        OrderRequestImpl executeOrder(OrderRequestImpl order, long price) {
            if (log.isDebugEnabled()) {
                log.debug "Order [${order.id}] can be filled, executing it."
            }
            return new OrderRequestImpl(order, OrderStatus.EXECUTED, currentDate, price)
        }

        OrderRequestImpl checkForCancellation(OrderRequestImpl order) {
            if (order.order.duration == Duration.DAY_ORDER) {
                if (log.isDebugEnabled()) {
                    log.debug "Order [${order.id}] is a day order, cancelling it."
                }
                return new OrderRequestImpl(order, OrderStatus.CANCELLED, currentDate)
            }

            Date oneYearAfterSubmission = new Date(order.submissionDate.time + TimeUtil.MILLIS_PER_YEAR)
            if (!oneYearAfterSubmission.after(currentDate)) {
                if (log.isDebugEnabled()) {
                    log.debug "Order [${order.id}] is more than one year old, cancelling it."
                }
                return new OrderRequestImpl(order, OrderStatus.CANCELLED, currentDate)
            }

            if (log.isDebugEnabled()) {
                log.debug "Order [${order.id}] is a GTC order, but not more than a year old, returning it to be tried tomorrow."
            }
            return order
        }
    }

    @EqualsAndHashCode(includes = ['id'])
    @ToString(includePackage = false)
    private static class OrderRequestImpl implements OrderRequest, Comparable<OrderRequest> {
        private static final String NOT_EXECUTED_PRICE = 'N/A'
        final Order order
        final String id
        final Date submissionDate
        final OrderStatus status
        final long executionPrice
        final Date processedDate

        private OrderRequestImpl(Order order, Date submissionDate) {
            this.id = UUID.randomUUID().toString()
            this.order = order
            this.status = OrderStatus.OPEN
            this.submissionDate = submissionDate
            this.processedDate = null
        }

        private OrderRequestImpl(OrderRequest order, OrderStatus newStatus, Date processedDate) {
            this(order, newStatus, processedDate, 0)
        }

        private OrderRequestImpl(OrderRequest order, OrderStatus newStatus, Date processedDate,
                                 final long executionPrice) {
            this.order = order
            this.id = order.id
            this.submissionDate = order.submissionDate
            this.status = newStatus
            this.executionPrice = executionPrice
            this.processedDate = processedDate
        }

        @Override
        int getShares() {
            return order.shares
        }

        @Override
        BaseSecurity getSecurity() {
            return order.security
        }

        @Override
        int compareTo(final OrderRequest o) {
            int comparison = submissionDate.compareTo(o.submissionDate)
            if (comparison == 0) {
                comparison = id.compareTo(o.id)
            }
            return comparison
        }
    }
}
