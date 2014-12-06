package com.jdom.bodycomposition.service

import com.jdom.bodycomposition.domain.YahooStockTicker
import com.jdom.bodycomposition.domain.YahooStockTickerData
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import javax.transaction.Transactional

/**
 * Created by djohnson on 11/14/14.
 */
@Service
@Transactional
class SimpleYahooStockTickerService implements YahooStockTickerService {

    @Autowired
    YahooStockTickerDao yahooStockTickerDao

    @Autowired
    YahooStockTickerDataDao yahooStockTickerDataDao

    @Autowired
    YahooStockTickerHistoryDownloader historyDownloader

    @Override
    List<YahooStockTicker> getTickers() {
        return yahooStockTickerDao.findAll()
    }

    @Override
    void updateHistoryData(YahooStockTicker ticker) throws FileNotFoundException {
        println "Updating history data for ticker ${ticker.ticker}"

        String historyData = historyDownloader.download(ticker)
        def parsedData = new YahooStockTickerHistoryParser(ticker, historyData).parse()
        println "Parsed ${parsedData.size()} entities for ticker ${ticker.ticker}"

        for (YahooStockTickerData data : parsedData) {
            yahooStockTickerDao.save(data)
        }
        println "Inserted ${parsedData.size()} entities for ticker ${ticker.ticker}"
    }
}
