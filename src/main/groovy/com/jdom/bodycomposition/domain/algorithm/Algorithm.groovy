package com.jdom.bodycomposition.domain.algorithm
import com.jdom.bodycomposition.domain.BaseSecurity
import com.jdom.bodycomposition.domain.DailySecurityData
import com.jdom.bodycomposition.domain.broker.Broker
/**
 * Created by djohnson on 12/6/14.
 */
interface Algorithm extends Serializable {

    boolean includeSecurity(BaseSecurity ticker)

    void actionsForDay(Broker broker, List<DailySecurityData> dayEntries, Date currentDate)
}
