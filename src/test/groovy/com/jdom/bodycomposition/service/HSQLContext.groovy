package com.jdom.bodycomposition.service

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType
/**
 * Created by djohnson on 11/15/14.
 */
@Profile(SpringProfiles.TEST)
@Configuration
class HSQLContext {
    @Bean
    public EmbeddedDatabase embeddedDatabase() {
        EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
        EmbeddedDatabase db = builder.setType(EmbeddedDatabaseType.HSQL).addScript("schema.sql")
              .addScript("security.sql").addScript("security_daily_data.sql").build();
        return db
    }
}
