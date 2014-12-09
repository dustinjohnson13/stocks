package com.jdom.bodycomposition.domain.algorithm.impl
import com.jdom.bodycomposition.domain.BaseSecurity
import com.jdom.bodycomposition.domain.DailySecurityData
import com.jdom.bodycomposition.domain.algorithm.Algorithm
import com.jdom.bodycomposition.domain.algorithm.BuyTransaction
import com.jdom.bodycomposition.domain.algorithm.Portfolio
import com.jdom.bodycomposition.domain.algorithm.PortfolioTransaction
import com.jdom.bodycomposition.domain.algorithm.SellTransaction

/**
 * Created by djohnson on 12/6/14.
 */
class BuyLowSellHighAlgorithm implements Algorithm {

    @Override
    boolean includeSecurity(final BaseSecurity ticker) {
        return ticker.symbol == 'MSFT'
    }

    @Override
    List<PortfolioTransaction> actionsForDay(Portfolio portfolio, final DailySecurityData dayEntry) {
        // Purchase when open is for $25.50
        if (dayEntry.open == 2550) {
//        (5,'1986-03-13',2550,2800,2925,2550,1031788800,7),
//        (5,'2003-11-28',2550,2571,2575,2540,33402600,1859),
//        (5,'2005-05-18',2550,2570,2584,2542,71182400,2090),
//        (5,'2010-07-14',2550,2544,2561,2512,72808100,2251),
//        (5,'2010-07-15',2550,2551,2559,2498,56934700,2257),
            return [new BuyTransaction(dayEntry.security, dayEntry.date, 2, 2550, portfolio.commissionCost)]
        // Sell when close is for $24.89
        } else if (dayEntry.close == 2489) {
//            (5,'2004-03-18',2496,2489,2503,2458,123231000,1799),
//            (5,'2010-07-16',2551,2489,2564,2488,65064800,2202),
//            (5,'2011-09-30',2520,2489,2550,2488,54060500,2270),
            return [new SellTransaction(dayEntry.security, dayEntry.date, 2, 2489, portfolio.commissionCost)]
        }
        return []
    }
}
