package com.jdom.bodycomposition.domain.algorithm
import com.jdom.bodycomposition.domain.BaseSecurity
import com.jdom.bodycomposition.domain.DailySecurityData
/**
 * Created by djohnson on 12/6/14.
 */
interface Algorithm extends Serializable {

    boolean includeSecurity(BaseSecurity ticker)

    List<PortfolioTransaction> actionsForDay(Portfolio portfolio, DailySecurityData dayEntry)
}
