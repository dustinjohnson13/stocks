package com.jdom.bodycomposition.domain.broker
import com.jdom.bodycomposition.domain.algorithm.BuyTransaction
import com.jdom.bodycomposition.domain.algorithm.Portfolio
import com.jdom.bodycomposition.domain.algorithm.PortfolioTransaction
import com.jdom.bodycomposition.domain.algorithm.SellTransaction
import com.jdom.bodycomposition.domain.algorithm.TransferTransaction
import com.jdom.bodycomposition.domain.market.Market
import com.jdom.bodycomposition.domain.market.OrderRequest
import com.jdom.bodycomposition.domain.market.orders.BuyLimitOrder
import com.jdom.bodycomposition.domain.market.orders.OneCancelsOther
import com.jdom.bodycomposition.domain.market.orders.Order
import com.jdom.bodycomposition.domain.market.orders.OrderRejectedException
import com.jdom.bodycomposition.domain.market.orders.SellLimitOrder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

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

    private static class DefaultBroker implements Broker {

        private static final Logger log = LoggerFactory.getLogger(DefaultBroker)

        private final Market market
        private Portfolio portfolio
        final long commissionCost
        private final List<Transaction> transactions = []
        private final Map<String, PortfolioTransaction> pendingTransactions = [:]


        DefaultBroker(Market market, Portfolio portfolio, long commissionCost) {
            this.market = market
            this.portfolio = portfolio
            this.commissionCost = commissionCost
        }

        Portfolio getPortfolio() {
            return portfolio
        }

        @Override
        List<PortfolioTransaction> getTransactions() {
            return Collections.unmodifiableList(transactions)
        }

        @Override
        OrderRequest submit(final Order order) throws OrderRejectedException {

            // Try to process the transaction before submitting to the market
            try {
                def transaction = createTransaction(order)

                // Dry run the transactions, including all current pending of the same type
                Portfolio pendingPortfolio = portfolio

                def pendingTransactionsOfSameType = pendingTransactions.findAll { it.value.class == transaction.class }
                pendingTransactionsOfSameType.each { id, pendingTransaction ->
                    pendingPortfolio = pendingTransaction.apply(pendingPortfolio, false)
                }
                pendingPortfolio = transaction.apply(pendingPortfolio, false)

                def pendingTransactionsOfDifferentType = pendingTransactions.findAll { it.value.class != transaction.class }
                pendingTransactionsOfDifferentType.each { id, pendingTransaction ->
                    pendingTransaction.apply(pendingPortfolio, false) // Run against each one individually to make sure it would be compatible
                }

                // This would be valid, store the pending transaction
                def submittedOrder = market.submit(this, order)
                pendingTransactions.put(submittedOrder.id, transaction)
                return submittedOrder
            } catch (Exception e) {
                throw new OrderRejectedException(e)
            }
        }

        @Override
        List<OrderRequest> submit(final OneCancelsOther oco) {
            return market.submit(this, oco)
        }

        @Override
        OrderRequest getOrder(final OrderRequest orderRequest) {
            return market.getOrder(orderRequest)
        }

        @Override
        OrderRequest cancel(final OrderRequest orderRequest) {
            return market.cancel(orderRequest)
        }

        BuyTransaction createTransaction(BuyLimitOrder limitOrder) {
            return new BuyTransaction(limitOrder.security, new Date(), limitOrder.shares, limitOrder.price,
                    commissionCost)
        }

        SellTransaction createTransaction(SellLimitOrder limitOrder) {
            return new SellTransaction(limitOrder.security, new Date(), limitOrder.shares, limitOrder.price,
                    commissionCost)
        }

        TransferTransaction createTransaction(Order order) {
            throw new UnsupportedOperationException('Currently only handle buy and sell limit orders!')
        }

        @Override
        void orderFilled(final OrderRequest order) {
            def transaction = pendingTransactions.remove(order.id)
            if (transaction == null) {
                log.error("Null transaction found for order ${order}!!! This shouldn't have happened!")
                return
            }

            try {
                def transactionForProcessedDate = transaction.forDate(order.processedDate)
                transactions.add(transactionForProcessedDate)
                portfolio = transactionForProcessedDate.apply(portfolio)
            } catch (IllegalArgumentException e) {
                log.error("Unable to apply transaction to the portfolio!!! This shouldn't have happened!", e)
            }
        }

        @Override
        void orderCancelled(final OrderRequest order) {
            pendingTransactions.remove(order.id)
        }
    }
}
