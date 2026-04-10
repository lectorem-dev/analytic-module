package ru.ya.analytic.adapters.out.clickhouse;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import ru.ya.libs.model.ReferedEvent;
import ru.ya.libs.model.RequestedEvent;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AnalyticsClickHouseRepositoryIT {

    @Container
    private static final GenericContainer<?> CLICKHOUSE =
            new GenericContainer<>(DockerImageName.parse("clickhouse/clickhouse-server:latest"))
                    .withEnv("CLICKHOUSE_USER", "clickhouse")
                    .withEnv("CLICKHOUSE_PASSWORD", "root")
                    .withExposedPorts(8123);

    private JdbcTemplate jdbcTemplate;
    private AnalyticsClickHouseRepository analyticsRepository;
    private DataLoadClickHouseRepository loadRepository;

    @BeforeAll
    void setUpContainerBackedRepositories() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.clickhouse.jdbc.ClickHouseDriver");
        dataSource.setUrl("jdbc:clickhouse://%s:%d/default?compress=false".formatted(
                CLICKHOUSE.getHost(),
                CLICKHOUSE.getMappedPort(8123)
        ));
        dataSource.setUsername("clickhouse");
        dataSource.setPassword("root");

        jdbcTemplate = new JdbcTemplate(dataSource);
        analyticsRepository = new AnalyticsClickHouseRepository(jdbcTemplate);
        loadRepository = new DataLoadClickHouseRepository(jdbcTemplate);

        jdbcTemplate.execute("CREATE DATABASE IF NOT EXISTS analytic");
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS analytic.requested (
                    event_date Date,
                    cid UUID,
                    mid UUID,
                    count UInt32
                )
                ENGINE = MergeTree()
                PARTITION BY toYYYYMM(event_date)
                ORDER BY (mid, cid, event_date)
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS analytic.refered (
                    event_date Date,
                    mid UUID,
                    count UInt32
                )
                ENGINE = MergeTree()
                PARTITION BY toYYYYMM(event_date)
                ORDER BY (mid, event_date)
                """);
    }

    @BeforeEach
    void cleanTables() {
        jdbcTemplate.execute("TRUNCATE TABLE analytic.requested");
        jdbcTemplate.execute("TRUNCATE TABLE analytic.refered");
    }

    @Test
    void repositoriesPersistEventsAndReturnCountsFromRealClickHouse() {
        UUID targetManufactureId = UUID.randomUUID();
        UUID otherManufactureId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 4, 10);

        loadRepository.loadRequested(new RequestedEvent(categoryId, targetManufactureId, 11, date));
        loadRepository.loadRequested(new RequestedEvent(categoryId, targetManufactureId, 5, date));
        loadRepository.loadRequested(new RequestedEvent(categoryId, otherManufactureId, 99, date));
        loadRepository.loadReferred(new ReferedEvent(targetManufactureId, 3, date));
        loadRepository.loadReferred(new ReferedEvent(targetManufactureId, 4, date));
        loadRepository.loadReferred(new ReferedEvent(otherManufactureId, 10, date));

        assertThat(analyticsRepository.getTotalCount(targetManufactureId)).isEqualTo(16L);
        assertThat(analyticsRepository.getReferCount(targetManufactureId)).isEqualTo(7);
    }

    @Test
    void averageRankMatchesCurrentSqlBehaviorAgainstRealClickHouse() {
        UUID targetManufactureId = UUID.randomUUID();
        UUID competitorA = UUID.randomUUID();
        UUID competitorB = UUID.randomUUID();
        UUID competitorC = UUID.randomUUID();
        UUID categoryOne = UUID.randomUUID();
        UUID categoryTwo = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 4, 10);

        loadRepository.loadRequested(new RequestedEvent(categoryOne, targetManufactureId, 100, date));
        loadRepository.loadRequested(new RequestedEvent(categoryOne, competitorA, 50, date));

        loadRepository.loadRequested(new RequestedEvent(categoryTwo, competitorB, 80, date));
        loadRepository.loadRequested(new RequestedEvent(categoryTwo, targetManufactureId, 60, date));
        loadRepository.loadRequested(new RequestedEvent(categoryTwo, competitorC, 40, date));

        Double averageRank = analyticsRepository.getAverageRank(targetManufactureId);

        assertThat(averageRank).isEqualTo(1.75d);
    }
}
