package com.jdom.bodycomposition.service

import com.jdom.bodycomposition.domain.YahooStockTicker

/**
 * Created by djohnson on 12/6/14.
 */
class MockYahooStockTickerHistoryDownloader implements YahooStockTickerHistoryDownloader {

    List<UpdateRequest> updateRequests = []

    @Override
    String download(YahooStockTicker ticker) {
        return download(ticker, null, null)
    }

    @Override
    String download(YahooStockTicker ticker, Date start, Date end) {
        updateRequests += new UpdateRequest(ticker: ticker, start: start, end: end)
        return ''
    }

    static class UpdateRequest {
        YahooStockTicker ticker
        Date start
        Date end
    }
}
