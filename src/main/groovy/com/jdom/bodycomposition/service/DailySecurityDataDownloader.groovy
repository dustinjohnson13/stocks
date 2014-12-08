package com.jdom.bodycomposition.service

import com.jdom.bodycomposition.domain.Stock

/**
 * Created by djohnson on 12/5/14.
 */
interface DailySecurityDataDownloader {

    String download(Stock security)

    String download(Stock security, Date start, Date end)
}
