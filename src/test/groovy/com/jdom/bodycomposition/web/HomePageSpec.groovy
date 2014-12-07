package com.jdom.bodycomposition.web

import static com.jdom.bodycomposition.web.StocksTester.StocksFormTester

import com.jdom.bodycomposition.service.MockYahooStockTickerHistoryDownloader
import com.jdom.bodycomposition.service.SpringProfiles
import com.jdom.bodycomposition.service.StocksContext
import com.jdom.util.TimeUtil
import com.jdom.util.TimeUtilHelper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

@ActiveProfiles(SpringProfiles.TEST)
@ContextConfiguration(classes = [StocksContext.class])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class HomePageSpec extends Specification {

    @Autowired
    WicketApplication application

    @Autowired
    MockYahooStockTickerHistoryDownloader historyDownloader

    StocksTester tester;

    HomePage page

    def setup() {
        tester = new StocksTester(application)
        page = tester.startPage(HomePage.class)
        tester.assertRenderedPage(HomePage.class)
    }

    def cleanup() {
        TimeUtilHelper.resumeTime()
    }

    def 'should update ticker data up to the present when the button is clicked'() {

        def expectedTickers = [
                'EBF',
                'FB',
                'GOOG',
                'KO',
                'MSFT',
                'YHOO'
        ]

        when: 'the update ticker data button is clicked'
        tester.clickLink('updateTickerData')

        def updateRequests = historyDownloader.updateRequests
        then: 'the ticker data was updated from the most recent date to today'
        updateRequests.size() == 6

        and: 'the update requests were made from the newest data to today'
        updateRequests.eachWithIndex { it, idx ->
            assert it.start == TimeUtil.newDateAtStartOfDay(2014, Calendar.DECEMBER, 6)
            assert it.end == null
            assert it.ticker.ticker == expectedTickers[idx]
        }
    }

    def 'should not update ticker data when data exists up to the current date'() {

        given: 'data is up to date for the tickers'
        TimeUtilHelper.freezeTime(TimeUtil.newDateAtStartOfDay(2014, Calendar.DECEMBER, 5))

        when: 'the update ticker data button is clicked'
        tester.clickLink('updateTickerData')

        def updateRequests = historyDownloader.updateRequests
        then: 'the ticker data was updated from the most recent date to today'
        updateRequests.isEmpty()
    }

    def 'should profile algorithm on button click'() {

//        PURCHASES:
//        (5,'2003-11-28',2550,2571,2575,2540,33402600,1859),
//        (5,'2005-05-18',2550,2570,2584,2542,71182400,2090),
//        (5,'2010-07-14',2550,2544,2561,2512,72808100,2251),
//        (5,'2010-07-15',2550,2551,2559,2498,56934700,2257)

//        SALES;
//        (5,'2004-03-18',2496,2489,2503,2458,123231000,1799),
//        (5,'2010-07-16',2551,2489,2564,2488,65064800,2202),

        StocksFormTester formTester = tester.newFormTester('algorithmProfilePanel:form')
        formTester.setValue('startDate', '11/27/2003')
        formTester.setValue('endDate', '07/17/2010')
        formTester.setMoney('portfolio.cash', '$200')
        formTester.setMoney('portfolio.commissionCost', '$5')

        when: 'the profile algorithm button is clicked'
        formTester.submit('profile')

        def portfolioCash = formTester.getTextComponentValue('portfolio.cash')

        then: 'the portfolio result was calculated'
        portfolioCash == '$65.56'
    }
}