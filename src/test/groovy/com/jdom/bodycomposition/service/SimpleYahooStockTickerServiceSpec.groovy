package com.jdom.bodycomposition.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.transaction.TransactionConfiguration
import spock.lang.Specification

import javax.transaction.Transactional
/**
 * Created by djohnson on 11/15/14.
 */
@ActiveProfiles(SpringProfiles.TEST)
@ContextConfiguration(classes = [StocksContext.class])
@Transactional
@TransactionConfiguration(defaultRollback = true)
class SimpleYahooStockTickerServiceSpec extends Specification {

    @Autowired YahooStockTickerService service

    def 'should return all stock tickers'() {

        when: 'all tickers are retrieved'
        def results = service.tickers
        def actualSize = results.size()

        then: 'the correct number of results were returned'
        actualSize == 13875
    }
}
