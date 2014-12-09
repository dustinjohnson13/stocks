package com.jdom.bodycomposition.web

import com.jdom.bodycomposition.service.StocksServiceContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
/**
 * Created by djohnson on 11/14/14.
 */
@ComponentScan(basePackageClasses = [StocksServiceContext.class, WicketApplication.class])
@Configuration
class StocksWebContext {

    private static final Logger LOG = LoggerFactory.getLogger(StocksWebContext.class)

    public StocksWebContext() {
        LOG.info('Context initialized.')
    }
}
