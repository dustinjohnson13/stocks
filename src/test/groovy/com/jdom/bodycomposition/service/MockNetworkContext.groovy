package com.jdom.bodycomposition.service

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
    public DailySecurityDataDownloader downloader() {
        return new MockDailySecurityDataDownloader()
    }
}
