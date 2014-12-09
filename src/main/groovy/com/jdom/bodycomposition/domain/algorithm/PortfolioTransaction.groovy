package com.jdom.bodycomposition.domain.algorithm
/**
 * Created by djohnson on 12/6/14.
 */
interface PortfolioTransaction extends Serializable {
    Portfolio apply(Portfolio portfolio)

    String getAction()

    String getSymbol()

    Date getDate()

    int getShares()

    long getPrice()

    long getCommission()

    long getCashValue()
}