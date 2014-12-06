package com.jdom.bodycomposition.service

import com.jdom.bodycomposition.domain.YahooStockTicker
/**
 * Created by djohnson on 11/14/14.
 */
interface YahooStockTickerService {
    List<YahooStockTicker> getTickers()

    void updateHistoryData(YahooStockTicker ticker) throws FileNotFoundException
}