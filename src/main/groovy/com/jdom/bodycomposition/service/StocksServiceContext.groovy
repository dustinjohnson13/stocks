package com.jdom.bodycomposition.service
import com.jdom.bodycomposition.domain.Stock
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
/**
 * Created by djohnson on 11/14/14.
 */
@ComponentScan(basePackageClasses = [Stock.class, DbContext.class])
@Configuration
class StocksServiceContext {

    private static final Logger LOG = LoggerFactory.getLogger(StocksServiceContext.class)

    public StocksServiceContext() {
        LOG.info('Context initialized.')
    }
}
