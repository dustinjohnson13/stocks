package com.jdom.bodycomposition.domain.broker

import com.jdom.bodycomposition.domain.algorithm.BuyTransaction
import com.jdom.bodycomposition.domain.algorithm.Portfolio
import com.jdom.bodycomposition.domain.algorithm.SellTransaction
import com.jdom.bodycomposition.domain.algorithm.TransferTransaction
import com.jdom.bodycomposition.domain.market.Market
import com.jdom.bodycomposition.domain.market.OrderProcessedListener
import com.jdom.bodycomposition.domain.market.OrderRequest
import com.jdom.bodycomposition.domain.market.orders.BuyLimitOrder
import com.jdom.bodycomposition.domain.market.orders.Order
import com.jdom.bodycomposition.domain.market.orders.OrderRejectedException
import com.jdom.bodycomposition.domain.market.orders.SellLimitOrder

import javax.transaction.Transaction

/**
 * Created by djohnson on 12/11/14.
 */
final class Brokers {

    private Brokers() {
    }

    static Broker create(final Market market, final Portfolio portfolio, final long commissionCost) {
        return new DefaultBroker(market, portfolio, commissionCost)
    }

    private static class DefaultBroker implements Broker, OrderProcessedListener {

        private final Market market
        private Portfolio portfolio
        final long commissionCost
        final List<Transaction> transactions = []
        final Map<String, TransferTransaction> pendingTransactions = [:]


        DefaultBroker(Market market, Portfolio portfolio, long commissionCost) {
            this.market = market
            this.portfolio = portfolio
            this.commissionCost = commissionCost

            market.registerOrderFilledListener(this)
        }

        public Portfolio getPortfolio() {
            return portfolio
        }

        @Override
        OrderRequest submit(final Order order) throws OrderRejectedException {

            // Try to process the transaction before submitting to the market
            try {
                def transaction = createTransaction(order)

                // Dry run the transactions, including all current pending
                Portfolio pendingPortfolio = portfolio
                pendingTransactions.each { id, pendingTransaction ->
                    pendingPortfolio = pendingTransaction.apply(pendingPortfolio, false)
                }
                pendingPortfolio = transaction.apply(pendingPortfolio, false)

                // This would be valid, store the pending transaction
                def submittedOrder = market.submit(order)
                pendingTransactions.put(submittedOrder.id, transaction)
                return submittedOrder
            } catch (Exception e) {
                throw new OrderRejectedException(e)
            }
        }

        @Override
        OrderRequest getOrder(final OrderRequest orderRequest) {
            return market.getOrder(orderRequest)
        }

        TransferTransaction createTransaction(BuyLimitOrder limitOrder) {
            return new BuyTransaction(limitOrder.security, new Date(), limitOrder.shares, limitOrder.price,
                    commissionCost)
        }

        TransferTransaction createTransaction(SellLimitOrder limitOrder) {
            return new SellTransaction(limitOrder.security, new Date(), limitOrder.shares, limitOrder.price,
                    commissionCost)
        }

        TransferTransaction createTransaction(Order order) {
            throw new UnsupportedOperationException('Currently only handle buy and sell limit orders!')
        }

        @Override
        void orderFilled(final OrderRequest order) {
            def transaction = pendingTransactions.remove(order.id)

            def transactionForProcessedDate = transaction.forDate(order.processedDate)
            transactions.add(transactionForProcessedDate)
            portfolio = transactionForProcessedDate.apply(portfolio)
        }

        @Override
        void orderCancelled(final OrderRequest order) {
            pendingTransactions.remove(order.id)
        }
    }
}
