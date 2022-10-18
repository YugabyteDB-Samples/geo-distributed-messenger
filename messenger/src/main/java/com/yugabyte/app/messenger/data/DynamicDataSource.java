package com.yugabyte.app.messenger.data;

import java.util.HashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.stereotype.Component;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/*
 * 
 * 
 * # JPA configuration
spring.jpa.properties.hibernate.dialect = org.hibernate.dialect.PostgreSQLDialect
spring.jpa.generate-ddl=false

# Schema initialization
spring.sql.init.mode=always
spring.sql.init.schema-locations=classpath:messenger_schema.sql 
spring.sql.init.continue-on-error=true

# HikariCP configuration
spring.datasource.hikari.maximum-pool-size=5
spring.datasource.hikari.schema=messenger

# PostgreSQL configuration for local deployments
spring.datasource.url = jdbc:postgresql://localhost:5432/postgres
spring.datasource.username = postgres
spring.datasource.password = password
 */
@Component
@Scope("singleton")
public class DynamicDataSource extends AbstractRoutingDataSource {

    public final static Integer CURRENT_DATA_SOURCE_KEY = 1;

    private String url;
    private String username;
    private String password;
    private String schemaName;
    private String yugabyteConnType;
    private int maxPoolSize;

    private HashMap<Object, Object> dataSources = new HashMap<>();

    public DynamicDataSource(
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password,
            @Value("${spring.datasource.hikari.maximum-pool-size:5}") int maxPoolSize,
            @Value("${spring.datasource.hikari.schema:public}") String schemaName,
            @Value("${yugabytedb.connection.type:standard}") String yugabyteConnType) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.maxPoolSize = maxPoolSize;
        this.schemaName = schemaName;
        this.yugabyteConnType = yugabyteConnType;

        setTargetDataSources(dataSources);

        initDataSource();

        afterPropertiesSet();
    }

    private void initDataSource() {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(url);
        cfg.setUsername(username);
        cfg.setPassword(password);
        cfg.setSchema(schemaName);
        cfg.setMaximumPoolSize(maxPoolSize);

        HikariDataSource ds = new HikariDataSource(cfg);

        dataSources.put(CURRENT_DATA_SOURCE_KEY, ds);
        setDefaultTargetDataSource(ds);

        System.out.printf("Initialized new data source for '%s' connection%n", url);
    }

    @Override
    protected Object determineCurrentLookupKey() {
        return CURRENT_DATA_SOURCE_KEY;
    }

    public void createNewDataSource(String url, String username, String password, String yugabyteConnType) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.yugabyteConnType = yugabyteConnType;

        initDataSource();
        afterPropertiesSet();
    }

}
