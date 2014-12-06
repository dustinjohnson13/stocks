package com.jdom.bodycomposition.service
import org.apache.commons.dbcp.BasicDataSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

import javax.sql.DataSource
/**
 * Created by djohnson on 11/15/14.
 */
@Profile(SpringProfiles.PRODUCTION)
@Configuration
class MysqlContext {
    @Bean
    public DataSource dataSource() {
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName("com.mysql.jdbc.Driver");
        dataSource.setUrl("jdbc:mysql://localhost:3306/stocks");
        dataSource.setUsername("root");
        dataSource.setPassword("");

        return dataSource;
    }
}
