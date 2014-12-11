package com.jdom.bodycomposition.domain.market

import javax.transaction.Transaction
/**
 * Created by djohnson on 12/10/14.
 */
interface MarketEngine extends Market {

    void processDay(Date date)

    List<Transaction> getTransactions()
}
