package com.jdom.bodycomposition.service
import com.jdom.bodycomposition.domain.YahooStockTicker
import com.jdom.bodycomposition.domain.algorithm.Algorithm
import com.jdom.bodycomposition.domain.algorithm.Portfolio

/**
 * Created by djohnson on 11/14/14.
 */
interface YahooStockTickerService {
    List<YahooStockTicker> getTickers()

    void updateHistoryData(YahooStockTicker ticker) throws FileNotFoundException

    Portfolio profileAlgorithm(Algorithm algorithm, Portfolio initialPortfolioAmount, Date startDate, Date endDate)
}