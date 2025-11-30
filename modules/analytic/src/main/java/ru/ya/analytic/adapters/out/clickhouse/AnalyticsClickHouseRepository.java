package ru.ya.analytic.adapters.out.clickhouse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.ya.analytic.application.out.AnalyticsPort;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AnalyticsClickHouseRepository implements AnalyticsPort {

    private final JdbcTemplate clickHouseJdbcTemplate;

    // ======================
    // EMBEDDED SQL QUERIES
    // ======================

    private static final String SQL_AVG_RANK = """
        SELECT avg(avg_rank) AS average_rank
        FROM (
                 SELECT
                     cid,
                     avg(rank) AS avg_rank
                 FROM (
                          SELECT
                              cid,
                              row_number() OVER (PARTITION BY cid ORDER BY count DESC) AS rank
                          FROM analytic.requested
                      ) AS ranked
                 WHERE cid IN (
                     SELECT DISTINCT cid
                     FROM analytic.requested
                     WHERE mid = ?
                 )
                 GROUP BY cid
             ) as car
        """;

    private static final String SQL_TOTAL_COUNT = """
        SELECT sum(count) AS total_count
        FROM analytic.requested
        WHERE mid = ?
        """;

    private static final String SQL_REFER_COUNT = """
        SELECT sum(count) AS refer_count
        FROM analytic.refered
        WHERE mid = ?
        """;

    // ======================
    //      METHODS
    // ======================

    @Override
    public Double getAverageRank(UUID mid) {
        log.debug("Executing AVG_RANK query for mid: {}", mid);
        Double result = clickHouseJdbcTemplate.queryForObject(
                SQL_AVG_RANK,
                new Object[]{mid.toString()},
                Double.class
        );
        return result != null ? result : 0.0;
    }

    @Override
    public Long getTotalCount(UUID mid) {
        log.debug("Executing TOTAL_COUNT query for mid: {}", mid);
        Long result = clickHouseJdbcTemplate.queryForObject(
                SQL_TOTAL_COUNT,
                new Object[]{mid.toString()},
                Long.class
        );
        return result != null ? result : 0L;
    }

    @Override
    public Integer getReferCount(UUID mid) {
        log.debug("Executing REFER_COUNT query for mid: {}", mid);
        Integer result = clickHouseJdbcTemplate.queryForObject(
                SQL_REFER_COUNT,
                new Object[]{mid.toString()},
                Integer.class
        );
        return result != null ? result : 0;
    }
}