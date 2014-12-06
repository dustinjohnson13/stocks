package com.jdom.bodycomposition.web

import com.jdom.bodycomposition.service.StocksContext
import com.jdom.bodycomposition.service.SpringProfiles
import org.apache.wicket.util.tester.WicketTester
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

@ActiveProfiles(SpringProfiles.TEST)
@ContextConfiguration(classes = [StocksContext.class])
class HomePageSpec extends Specification {

    @Autowired
    WicketApplication application

    WicketTester tester;

    def setup() {
        tester = new WicketTester(application)
    }

    def 'should render the homepage'() {

        when: 'the home page is started'
        tester.startPage(HomePage.class)

        then: 'the home page was rendered'
        tester.assertRenderedPage(HomePage.class)
    }
}
