package com.jdom.bodycomposition.service

import com.jdom.bodycomposition.domain.Stock

/**
 * Created by djohnson on 12/6/14.
 */
class MockDailySecurityDataDownloader implements DailySecurityDataDownloader {

    List<UpdateRequest> updateRequests = []

    @Override
    String download(Stock ticker) {
        return download(ticker, null, null)
    }

    @Override
    String download(Stock ticker, Date start, Date end) {
        updateRequests += new UpdateRequest(ticker: ticker, start: start, end: end)
        return ''
    }

    static class UpdateRequest {
        Stock ticker
        Date start
        Date end
    }
}
