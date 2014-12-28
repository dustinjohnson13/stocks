package com.jdom.bodycomposition.domain.market

import com.jdom.bodycomposition.domain.DailySecurityData

/**
 * Created by djohnson on 12/10/14.
 */
interface MarketEngine extends Market {

    void processDay(Date date, Map<Integer, DailySecurityData> dailySecurityDatas)
}
