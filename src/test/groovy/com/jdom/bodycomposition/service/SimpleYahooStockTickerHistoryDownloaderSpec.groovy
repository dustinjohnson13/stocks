package com.jdom.bodycomposition.service

import com.jdom.bodycomposition.domain.YahooStockTicker
import com.jdom.util.TimeUtil
import spock.lang.Specification

/**
 * Created by djohnson on 12/5/14.
 */
class SimpleYahooStockTickerHistoryDownloaderSpec extends Specification {

    def 'should download history data with specific dates'() {

        Date start = TimeUtil.newDateAtStartOfDay(2014, Calendar.APRIL, 14)
        Date end = TimeUtil.newDateAtStartOfDay(2014, Calendar.DECEMBER, 6)

        when: 'history data is downloaded for a stock ticker with specific dates'
        String historyData = new SimpleYahooStockTickerHistoryDownloader().download(new YahooStockTicker(ticker: 'YHOO'), start, end)

        then: 'the data contains the expected time range'
        historyData == SimpleYahooStockTickerHistoryDownloaderSpec.class.getResourceAsStream('/yhoo_20140414-20141206.csv').text
    }
}
