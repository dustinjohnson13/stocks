package com.jdom.bodycomposition.service

import com.jdom.bodycomposition.domain.Stock
import com.jdom.bodycomposition.domain.DailySecurityData
import com.jdom.util.TimeUtil
import spock.lang.Specification
/**
 * Created by djohnson on 12/5/14.
 */
class YahooDailySecurityDataParserSpec extends Specification {

    def 'should parse history data correctly'() {

        def Stock ticker = new Stock(symbol: 'YHOO')
        def expectedEntryAttributes = [
            [ticker, TimeUtil.newDateAtStartOfDay(2014, Calendar.APRIL, 14), 3355, 3345, 3404, 3304, 26322600, 3345],
            [ticker, TimeUtil.newDateAtStartOfDay(2014, Calendar.APRIL, 15), 3393, 3421, 3428, 3264, 50600400, 3421]
        ]

        Date expectedFirstEntryDate = TimeUtil.newDateAtStartOfDay(2014, Calendar.APRIL, 12)
        Date expectedLastEntryDate = TimeUtil.newDateAtStartOfDay(2014, Calendar.DECEMBER, 6)

        String historyData = YahooDailySecurityDataParserSpec.class.getResourceAsStream('/yhoo_20140414-20141206.csv').text

        when: 'history data is parsed'
        List<DailySecurityData> parsedData = new YahooDailySecurityDataParser(ticker, historyData).parse()
        def actualSize = parsedData.size()

        then: 'the parsed data contains the expected number of entries'
        actualSize == 165

        and: 'the entries have the correct attributes'
        expectedEntryAttributes.eachWithIndex { expected, index ->
            def parsed = parsedData[index]

            int idx = 0
            assert parsed.security.is(expected[idx++])
            assert parsed.date == expected[idx++]
            assert parsed.open == expected[idx++]
            assert parsed.close == expected[idx++]
            assert parsed.high == expected[idx++]
            assert parsed.low == expected[idx++]
            assert parsed.volume == expected[idx++]
            assert parsed.adjustedClose == expected[idx++]
        }
    }
}
