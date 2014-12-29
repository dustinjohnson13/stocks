package com.jdom.bodycomposition.domain.broker

import com.jdom.bodycomposition.domain.algorithm.Portfolio
import com.jdom.bodycomposition.domain.algorithm.PortfolioTransaction
import com.jdom.bodycomposition.domain.market.OrderRequest
import com.jdom.bodycomposition.domain.market.orders.OneCancelsOther
import com.jdom.bodycomposition.domain.market.orders.Order

/**
 * Created by djohnson on 12/10/14.
 */
interface Broker {

    OrderRequest submit(Order order)

    List<OrderRequest> submit(OneCancelsOther oco)

    OrderRequest getOrder(OrderRequest orderRequest)

    OrderRequest cancel(OrderRequest orderRequest)

    Portfolio getPortfolio()

    long getCommissionCost()

    List<PortfolioTransaction> getTransactions()

    void orderFilled(final OrderRequest order)

    void orderCancelled(final OrderRequest order)
}
