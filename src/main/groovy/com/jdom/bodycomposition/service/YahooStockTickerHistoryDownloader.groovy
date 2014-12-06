package com.jdom.bodycomposition.service

import com.jdom.bodycomposition.domain.YahooStockTicker

/**
 * Created by djohnson on 12/5/14.
 */
interface YahooStockTickerHistoryDownloader {

    String download(YahooStockTicker ticker)

    String download(YahooStockTicker ticker, Date start, Date end)
}
