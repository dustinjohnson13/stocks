package com.jdom.bodycomposition.web

import com.jdom.bodycomposition.domain.algorithm.impl.TestMsftAlgorithm
import com.jdom.bodycomposition.service.MockDailySecurityDataDownloader
import com.jdom.bodycomposition.service.SpringProfiles
import com.jdom.util.TimeUtil
import com.jdom.util.TimeUtilHelper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.transaction.TransactionConfiguration
import spock.lang.Specification

import javax.transaction.Transactional

@ActiveProfiles(SpringProfiles.TEST)
@ContextConfiguration(classes = [StocksWebContext.class])
@Transactional
@TransactionConfiguration(defaultRollback = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class HomePageSpec extends Specification {

    @Autowired
    WicketApplication application

    @Autowired
    MockDailySecurityDataDownloader historyDownloader

    StocksTester tester

    HomePage page

    def setup() {
        tester = new StocksTester(application)
        page = tester.startPage(HomePage.class)
        tester.assertRenderedPage(HomePage.class)
        page.scenarioModel.getObject().algorithmFactory = TestMsftAlgorithm.factory
    }

    def cleanup() {
        TimeUtilHelper.resumeTime()
    }

    def 'should update daily security data up to the present when the button is clicked'() {

        def expectedTickers = [
                'EBF',
                'FB',
                'GOOG',
                'KO',
                'MSFT',
                'YHOO'
        ]

        when: 'the update daily security data button is clicked'
        tester.clickLink('updateSecurityDailyData')

        def updateRequests = historyDownloader.updateRequests
        then: 'the ticker data was updated from the most recent date to today'
        updateRequests.size() == 6

        and: 'the update requests were made from the newest data to today'
        updateRequests.eachWithIndex { it, idx ->
            assert it.start == TimeUtil.newDateAtStartOfDay(2014, Calendar.DECEMBER, 6)
            assert it.end == null
            assert it.ticker.symbol == expectedTickers[idx]
        }
    }

    def 'should not update ticker data when data exists up to the current date'() {

        given: 'data is up to date for the tickers'
        TimeUtilHelper.freezeTime(TimeUtil.newDateAtStartOfDay(2014, Calendar.DECEMBER, 5))

        when: 'the update ticker data button is clicked'
        tester.clickLink('updateSecurityDailyData')

        def updateRequests = historyDownloader.updateRequests
        then: 'the ticker data was updated from the most recent date to today'
        updateRequests.isEmpty()
    }
}
