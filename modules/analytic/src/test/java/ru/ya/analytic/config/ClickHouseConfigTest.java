package ru.ya.analytic.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ClickHouseConfigTest {

    @Test
    void clickHouseDataSourceUsesConfiguredProperties() {
        ClickHouseConfig config = new ClickHouseConfig();
        ReflectionTestUtils.setField(config, "url", "jdbc:clickhouse://localhost:8123/default?compress=false");
        ReflectionTestUtils.setField(config, "username", "clickhouse");
        ReflectionTestUtils.setField(config, "password", "root");
        ReflectionTestUtils.setField(config, "driverClassName", "com.clickhouse.jdbc.ClickHouseDriver");

        DataSource dataSource = config.clickHouseDataSource();

        assertThat(dataSource).isInstanceOf(DriverManagerDataSource.class);
        DriverManagerDataSource driverManagerDataSource = (DriverManagerDataSource) dataSource;
        assertThat(driverManagerDataSource.getUrl()).isEqualTo("jdbc:clickhouse://localhost:8123/default?compress=false");
        assertThat(driverManagerDataSource.getUsername()).isEqualTo("clickhouse");
        assertThat(driverManagerDataSource.getPassword()).isEqualTo("root");
    }

    @Test
    void clickHouseInitializerCreatesDatabaseAndTables() throws Exception {
        ClickHouseConfig config = new ClickHouseConfig();
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ApplicationRunner runner = config.clickHouseInitializer(jdbcTemplate);

        runner.run(mock(ApplicationArguments.class));

        verify(jdbcTemplate).execute("CREATE DATABASE IF NOT EXISTS analytic");
        verify(jdbcTemplate).execute(contains("CREATE TABLE IF NOT EXISTS analytic.requested"));
        verify(jdbcTemplate).execute(contains("CREATE TABLE IF NOT EXISTS analytic.refered"));
    }
}
