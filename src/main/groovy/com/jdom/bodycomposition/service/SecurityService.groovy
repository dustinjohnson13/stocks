package com.jdom.bodycomposition.service
import com.jdom.bodycomposition.domain.BaseSecurity
import com.jdom.bodycomposition.domain.Stock
import com.jdom.bodycomposition.domain.algorithm.AlgorithmScenario
import com.jdom.bodycomposition.domain.algorithm.Portfolio
import com.jdom.bodycomposition.domain.algorithm.PortfolioValue

/**
 * Created by djohnson on 11/14/14.
 */
interface SecurityService {
    List<Stock> getStocks()

    void updateHistoryData(BaseSecurity security) throws FileNotFoundException

    AlgorithmScenario profileAlgorithm(AlgorithmScenario scenario)

    PortfolioValue portfolioValue(Portfolio portfolio, Date date)
}