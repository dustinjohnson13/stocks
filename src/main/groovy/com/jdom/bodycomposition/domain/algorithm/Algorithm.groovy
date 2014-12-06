package com.jdom.bodycomposition.domain.algorithm

import com.jdom.bodycomposition.domain.YahooStockTicker
import com.jdom.bodycomposition.domain.YahooStockTickerData

/**
 * Created by djohnson on 12/6/14.
 */
interface Algorithm {

    boolean includeTicker(YahooStockTicker ticker)

    List<TickerAction> actionsForDay(Portfolio portfolio, YahooStockTickerData dayEntry)
}
