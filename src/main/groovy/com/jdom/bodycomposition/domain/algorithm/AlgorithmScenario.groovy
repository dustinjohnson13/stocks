package com.jdom.bodycomposition.domain.algorithm

import groovy.transform.ToString

/**
 * Created by djohnson on 12/6/14.
 */
@ToString
class AlgorithmScenario implements Serializable {

    Portfolio portfolio

    Date startDate

    Date endDate

    Algorithm algorithm

}
