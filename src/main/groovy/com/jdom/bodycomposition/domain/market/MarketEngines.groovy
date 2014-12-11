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

/**
 * Created by djohnson on 12/10/14.
 */
final class MarketEngines {

    private MarketEngines() {
    }

    static MarketEngine create(final Date start, final Date end, final DailySecurityDataDao dailySecurityDataDao) {
        return new DailyDataDrivenMarketEngine(start, end, dailySecurityDataDao)
    }

    private static class DailyDataDrivenMarketEngine implements MarketEngine {
        private final List<OrderRequestImpl> orders = []
        final Date start
        final Date end
        final DailySecurityDataDao dailySecurityDataDao
        private Date currentDate

        private DailyDataDrivenMarketEngine(
                final Date start, final Date end, final DailySecurityDataDao dailySecurityDataDao) {
            this.start = start
            this.end = end
            this.dailySecurityDataDao = dailySecurityDataDao
            this.currentDate = start
        }

        @Override
        OrderRequest submit(final Order marketOrder) {
            def orderRequest = new OrderRequestImpl(marketOrder, currentDate)
            orders.add(orderRequest)

            return orderRequest
        }

        @Override
        void processDay() {

            List<OrderRequestImpl> origList = new ArrayList<>(orders)
            orders.clear()

            boolean anySecuritiesProcessed = false
            for (OrderRequestImpl order : origList) {
                def securityData = dailySecurityDataDao.findBySecurityAndDate(order.security, currentDate)
                if (securityData == null) {
                    continue;
                } else {
                    anySecuritiesProcessed = true
                }
                switch (order.order) {
                    case LimitOrder:
                        orders.add(processLimitOrder(order, order.order, securityData))
                        break;
                    case MarketOrder:
                        MarketOrder marketOrder = (MarketOrder) order.order
                        orders.add(new OrderRequestImpl(order, OrderStatus.EXECUTED, securityData.open))
                        break;
                }
            }

            // Must be non-market day, restore orders list
            if (!anySecuritiesProcessed) {
                orders.addAll(origList)
            }

            currentDate = new Date(currentDate.time + TimeUtil.MILLIS_PER_DAY)
        }

        @Override
        OrderRequest getOrder(final OrderRequest orderRequest) {
            return orders.find { it.id == orderRequest.id }
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
                    return new OrderRequestImpl(order, OrderStatus.CANCELLED)
                } else {

                    Date oneYearAfterSubmission = new Date(order.submissionDate.time + TimeUtil.MILLIS_PER_YEAR)
                    if (!oneYearAfterSubmission.after(currentDate)) {
                        return new OrderRequestImpl(order, OrderStatus.CANCELLED)
                    }
                }
                return order
            }
            return new OrderRequestImpl(order, OrderStatus.EXECUTED, limitOrder.price)
        }

    }

    private static class OrderRequestImpl implements OrderRequest {
        private final Order order
        final String id = UUID.randomUUID().toString()
        final Date submissionDate
        final OrderStatus status
        final long executionPrice

        private OrderRequestImpl(Order order, Date submissionDate) {
            this.order = order
            this.status = OrderStatus.OPEN
            this.submissionDate = submissionDate
        }

        private OrderRequestImpl(OrderRequest order, OrderStatus newStatus) {
            this(order, newStatus, Long.MIN_VALUE)
        }

        private OrderRequestImpl(OrderRequest order, OrderStatus newStatus, final long executionPrice) {
            this.order = order
            this.id = order.id
            this.submissionDate = order.submissionDate
            this.status = newStatus
            this.executionPrice = executionPrice
        }

        @Override
        int getShares() {
            return order.shares
        }

        @Override
        BaseSecurity getSecurity() {
            return order.security
        }
    }
}
