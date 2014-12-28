package com.jdom.bodycomposition.domain.market

import com.jdom.bodycomposition.domain.algorithm.Algorithm

/**
 * Created by djohnson on 12/29/14.
 */
interface AlgorithmFactory {
    Algorithm createInstance(int identifier)
}