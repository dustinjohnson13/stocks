package com.jdom.bodycomposition.domain.market

import com.jdom.bodycomposition.domain.BaseSecurity
import com.jdom.bodycomposition.domain.DailySecurityData
import com.jdom.bodycomposition.domain.market.orders.BuyLimitOrder
import com.jdom.bodycomposition.domain.market.orders.Duration
import com.jdom.bodycomposition.domain.market.orders.LimitOrder
import com.jdom.bodycomposition.domain.market.orders.MarketOrder
import com.jdom.bodycomposition.domain.market.orders.Order
import com.jdom.bodycomposition.domain.market.orders.SellLimitOrder
import com.jdom.bodycomposition.service.DailySecurityDataDao
import com.jdom.util.TimeUtil
import groovy.transform.EqualsAndHashCode

/**
 * Created by djohnson on 12/10/14.
 */
final class MarketEngines {

    private static final long COMMISSION_PRICE = 495l

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
            def orderRequest = new OrderRequestImpl(marketOrder, currentDate)
            openOrders.add(orderRequest)

            return orderRequest
        }

        @Override
        void processDay(Date date) {
            currentDate = date

            Set<OrderRequestImpl> origList = new HashSet<>(openOrders)
            openOrders.clear()

            boolean anySecuritiesProcessed = false
            for (OrderRequestImpl order : origList) {
                switch (order.status) {
                    // Skip these orders, they're done types
                    case OrderStatus.EXECUTED:
                    case OrderStatus.CANCELLED:
                    case OrderStatus.REJECTED:
                        throw new IllegalStateException("Found processed order ${order.id} in the open orders set!  This indicates a programming error!")
                    default:
                        break;
                }

                def securityData = dailySecurityDataDao.findBySecurityAndDate(order.security, date)
                if (securityData == null) {
                    continue;
                } else {
                    anySecuritiesProcessed = true
                }

                def processedOrder = order
                switch (order.order) {
                    case LimitOrder:
                        processedOrder = processLimitOrder(order, order.order, securityData)
                        break;
                    case MarketOrder:
                        MarketOrder marketOrder = (MarketOrder) order.order
                        processedOrder = new OrderRequestImpl(order, OrderStatus.EXECUTED, currentDate, securityData.open)
                        break;
                }

                if (processedOrder.status == OrderStatus.EXECUTED || processedOrder.status == OrderStatus.CANCELLED) {
                    processedOrders.add(processedOrder)
                    notifyListeners(processedOrder)
                } else {
                    openOrders.add(processedOrder)
                }
            }

            // Must be non-market day, restore orders list
            if (!anySecuritiesProcessed) {
                openOrders.addAll(origList)
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
            listeners.each {
                if (processedOrder.status == OrderStatus.EXECUTED) {
                    it.orderFilled(processedOrder)
                } else if (processedOrder.status == OrderStatus.CANCELLED) {
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

        OrderRequestImpl processLimitOrder(OrderRequestImpl order, LimitOrder limitOrder, Closure<Boolean> unfillable) {
            if (unfillable.call()) {
                if (limitOrder.duration == Duration.DAY_ORDER) {
                    return new OrderRequestImpl(order, OrderStatus.CANCELLED, currentDate)
                } else {

                    Date oneYearAfterSubmission = new Date(order.submissionDate.time + TimeUtil.MILLIS_PER_YEAR)
                    if (!oneYearAfterSubmission.after(currentDate)) {
                        return new OrderRequestImpl(order, OrderStatus.CANCELLED, currentDate)
                    }
                }
                return order
            }

            return new OrderRequestImpl(order, OrderStatus.EXECUTED, currentDate, limitOrder.price)
        }

    }

    @EqualsAndHashCode(includes = ['id'])
    private static class OrderRequestImpl implements OrderRequest, Comparable<OrderRequest> {
        private final Order order
        final String id = UUID.randomUUID().toString()
        final Date submissionDate
        final OrderStatus status
        final long executionPrice
        final Date processedDate

        private OrderRequestImpl(Order order, Date submissionDate) {
            this.order = order
            this.status = OrderStatus.OPEN
            this.submissionDate = submissionDate
            this.processedDate = null
        }

        private OrderRequestImpl(OrderRequest order, OrderStatus newStatus, Date processedDate) {
            this(order, newStatus, processedDate, Long.MIN_VALUE)
        }

        private OrderRequestImpl(OrderRequest order, OrderStatus newStatus, Date processedDate, final long executionPrice) {
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
            return getSubmissionDate().compareTo(o.getSubmissionDate())
        }
    }
}
