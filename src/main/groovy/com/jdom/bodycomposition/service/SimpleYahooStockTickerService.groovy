package com.jdom.bodycomposition.service
import com.jdom.bodycomposition.domain.YahooStockTicker
import com.jdom.bodycomposition.domain.YahooStockTickerData
import com.jdom.bodycomposition.domain.algorithm.Algorithm
import com.jdom.bodycomposition.domain.algorithm.Portfolio
import com.jdom.bodycomposition.domain.algorithm.TickerAction
import com.jdom.util.TimeUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service

import javax.transaction.Transactional
/**
 * Created by djohnson on 11/14/14.
 */
@Service
@Transactional
class SimpleYahooStockTickerService implements YahooStockTickerService {

    private static final Logger log = LoggerFactory.getLogger(SimpleYahooStockTickerService)

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
        log.info("Updating history data for ticker ${ticker.ticker}")

        Date start = null

        Pageable latest = new PageRequest(0, 1, Sort.Direction.DESC, 'date')
        Page<YahooStockTickerData> page = yahooStockTickerDataDao.findByTickerOrderByDateDesc(ticker, latest)

        if (page.hasContent()) {
            Date latestDate = TimeUtil.dateAtStartOfDay(page.getContent()[0].date)
            Date startOfToday = TimeUtil.dateAtStartOfDay(TimeUtil.newDate())

            if (!latestDate.before(startOfToday)) {
                return
            }

            Calendar cal = Calendar.getInstance()
            cal.setTime(latestDate)
            cal.add(Calendar.DATE, 1)
            start = cal.getTime()
        }

        String historyData = historyDownloader.download(ticker, start, null)
        def parsedData = new YahooStockTickerHistoryParser(ticker, historyData).parse()
        log.info("Parsed ${parsedData.size()} entities for ticker ${ticker.ticker}")

        for (YahooStockTickerData data : parsedData) {
            yahooStockTickerDao.save(data)
        }
        log.info("Inserted ${parsedData.size()} entities for ticker ${ticker.ticker}")
    }

    @Override
    Portfolio profileAlgorithm(final Algorithm algorithm, final Portfolio initialPortfolio,
                          final Date startDate, final Date endDate) {
        Portfolio portfolio = initialPortfolio
        List<YahooStockTicker> tickers = getTickers()
        for (Iterator<YahooStockTicker> iter = tickers.iterator(); iter.hasNext();) {
            if (!algorithm.includeTicker(iter.next())) {
                iter.remove()
            }
        }

        for (YahooStockTicker ticker : tickers) {
            def dayEntries = yahooStockTickerDataDao.findByTickerAndDateBetween(ticker, startDate, endDate)

            for (YahooStockTickerData dayEntry : dayEntries) {
                List<TickerAction> actions = algorithm.actionsForDay(portfolio, dayEntry)
                if (!actions.empty) {
                    actions.each {
                        portfolio = it.apply(portfolio)
                    }
                }
            }
        }

        return portfolio
    }
}
