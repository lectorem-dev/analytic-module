package ru.ya.analytic.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

@Configuration
@Slf4j
public class ClickHouseConfig {

    @Value("${spring.datasource.clickhouse.url}")
    private String url;

    @Value("${spring.datasource.clickhouse.username}")
    private String username;

    @Value("${spring.datasource.clickhouse.password}")
    private String password;

    @Value("${spring.datasource.clickhouse.driver-class-name}")
    private String driverClassName;

    @Bean
    public DataSource clickHouseDataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setDriverClassName(driverClassName);
        log.info("ClickHouse DataSource initialized: {}", url);
        return dataSource;
    }

    @Bean
    public JdbcTemplate clickHouseJdbcTemplate(DataSource clickHouseDataSource) {
        return new JdbcTemplate(clickHouseDataSource);
    }

    // Создание таблиц через ApplicationRunner вместо PostConstruct
    @Bean
    public ApplicationRunner clickHouseInitializer(JdbcTemplate clickHouseJdbcTemplate) {
        return args -> {
            log.info("Checking/creating ClickHouse analytics tables...");

            clickHouseJdbcTemplate.execute("CREATE DATABASE IF NOT EXISTS analytic");

            String createRequested = """
                CREATE TABLE IF NOT EXISTS analytic.requested (
                    event_date Date,
                    cid UUID,
                    mid UUID,
                    count UInt32
                )
                ENGINE = MergeTree()
                PARTITION BY toYYYYMM(event_date)
                ORDER BY (mid, cid, event_date)
                """;

            String createRefered = """
                CREATE TABLE IF NOT EXISTS analytic.refered (
                    event_date Date,
                    mid UUID,
                    count UInt32
                )
                ENGINE = MergeTree()
                PARTITION BY toYYYYMM(event_date)
                ORDER BY (mid, event_date)
                """;

            clickHouseJdbcTemplate.execute(createRequested);
            clickHouseJdbcTemplate.execute(createRefered);

            log.info("ClickHouse tables analytic.requested & analytic.refered are ready.");
        };
    }
}