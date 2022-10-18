package com.yugabyte.app.messenger.data;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class DynamicDataSourceConfig {

    @Autowired
    private DynamicDataSource dynamicDataSource;

    @Bean
    @Primary
    public DataSource dataSource() {
        return dynamicDataSource;
    }
}
