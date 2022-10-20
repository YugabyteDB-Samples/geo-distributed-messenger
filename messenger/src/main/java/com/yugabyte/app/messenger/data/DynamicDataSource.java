package com.yugabyte.app.messenger.data;

import java.sql.SQLException;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.stereotype.Component;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@Component
@Scope("singleton")
public class DynamicDataSource extends AbstractRoutingDataSource {

    private enum YugabyteConnectionType {
        STANDARD, REPLICA, GEO
    }

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

        if (isReplicaConnection()) {
            System.out.println("Setting read only session characteristics for the replica connection");

            cfg.setConnectionInitSql(
                    "set session characteristics as transaction read only;" +
                            "set yb_read_from_followers = true;");
        }

        // if (isGeoPartitionedConnection()) {
        // System.out.println("Allowing global transactions for the geo-partitioned
        // connection");

        // cfg.setConnectionInitSql(
        // "SET force_global_transaction = TRUE;");
        // }

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

    public boolean isReplicaConnection() {
        return yugabyteConnType.equalsIgnoreCase(YugabyteConnectionType.REPLICA.name());
    }

    private boolean isGeoPartitionedConnection() {
        return yugabyteConnType.equalsIgnoreCase(YugabyteConnectionType.GEO.name());
    }
}
