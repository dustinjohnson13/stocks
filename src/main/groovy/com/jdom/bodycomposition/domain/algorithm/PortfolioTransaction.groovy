package com.jdom.bodycomposition.domain.algorithm
/**
 * Created by djohnson on 12/6/14.
 */
interface PortfolioTransaction extends Serializable {
    Portfolio apply(Portfolio portfolio)

    Portfolio apply(Portfolio portfolio, boolean logTransaction)

    String getAction()

    String getSymbol()

    Date getDate()

    int getShares()

    long getPrice()

    long getCommission()

    long getCashValue()

    PortfolioTransaction forDate(Date date)
}