package com.jdom.bodycomposition.domain.algorithm

import groovy.transform.ToString

/**
 * Created by djohnson on 12/6/14.
 */
@ToString
class AlgorithmScenario implements Serializable {

    Portfolio initialPortfolio

    Date startDate

    Date endDate

    Algorithm algorithm

    List<PortfolioTransaction> transactions = []

    PortfolioValue resultPortfolio

    long valueChangePercent

    long duration
}
