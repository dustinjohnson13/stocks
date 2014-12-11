package com.jdom.bodycomposition.domain.algorithm
import com.jdom.bodycomposition.domain.BaseSecurity
import com.jdom.bodycomposition.domain.DailySecurityData
import com.jdom.bodycomposition.domain.market.Market

/**
 * Created by djohnson on 12/6/14.
 */
interface Algorithm extends Serializable {

    boolean includeSecurity(BaseSecurity ticker)

    void actionsForDay(Market market, DailySecurityData dayEntry)
}
