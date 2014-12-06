package com.jdom.bodycomposition.service

import com.jdom.bodycomposition.domain.YahooStockTicker
import com.jdom.bodycomposition.web.WicketApplication
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

/**
 * Created by djohnson on 11/14/14.
 */
@ComponentScan(basePackageClasses = [YahooStockTicker.class, DbContext.class, WicketApplication.class])
@Configuration
class StocksContext {

    private static final Logger LOG = LoggerFactory.getLogger(StocksContext.class)

    public StocksContext() {
        LOG.info('Context initialized.')
    }
}
