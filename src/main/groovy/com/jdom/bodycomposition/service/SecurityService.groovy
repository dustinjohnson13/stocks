package com.jdom.bodycomposition.service

import com.jdom.bodycomposition.domain.BaseSecurity
import com.jdom.bodycomposition.domain.Stock
import com.jdom.bodycomposition.domain.algorithm.Algorithm
import com.jdom.bodycomposition.domain.algorithm.AlgorithmScenario

/**
 * Created by djohnson on 11/14/14.
 */
interface SecurityService {
    List<Stock> getStocks()

    void updateHistoryData(BaseSecurity security) throws FileNotFoundException

    AlgorithmScenario profileAlgorithm(Algorithm algorithm, AlgorithmScenario scenario)
}