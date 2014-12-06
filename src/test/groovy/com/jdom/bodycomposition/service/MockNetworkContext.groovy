package com.jdom.bodycomposition.service

import com.jdom.bodycomposition.domain.YahooStockTicker
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

/**
 * Created by djohnson on 11/15/14.
 */
@Profile(SpringProfiles.TEST)
@Configuration
class MockNetworkContext {
    @Bean
    public YahooStockTickerHistoryDownloader downloader() {
        return new YahooStockTickerHistoryDownloader() {
            @Override
            String download(YahooStockTicker ticker) {
                return ""
            }

            @Override
            String download(YahooStockTicker ticker, Date start, Date end) {
                return ""
            }
        }
    }
}
