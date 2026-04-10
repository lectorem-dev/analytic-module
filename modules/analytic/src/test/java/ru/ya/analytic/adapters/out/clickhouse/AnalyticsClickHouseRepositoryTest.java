package ru.ya.analytic.adapters.out.clickhouse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsClickHouseRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    void getAverageRankReturnsJdbcValue() {
        AnalyticsClickHouseRepository repository = new AnalyticsClickHouseRepository(jdbcTemplate);
        UUID manufactureId = UUID.randomUUID();
        when(jdbcTemplate.queryForObject(anyString(), any(Object[].class), eq(Double.class))).thenReturn(9.5);

        Double result = repository.getAverageRank(manufactureId);

        assertThat(result).isEqualTo(9.5);
    }

    @Test
    void getAverageRankFallsBackToZeroWhenJdbcReturnsNull() {
        AnalyticsClickHouseRepository repository = new AnalyticsClickHouseRepository(jdbcTemplate);
        when(jdbcTemplate.queryForObject(anyString(), any(Object[].class), eq(Double.class))).thenReturn(null);

        Double result = repository.getAverageRank(UUID.randomUUID());

        assertThat(result).isZero();
    }

    @Test
    void getTotalCountFallsBackToZeroWhenJdbcReturnsNull() {
        AnalyticsClickHouseRepository repository = new AnalyticsClickHouseRepository(jdbcTemplate);
        when(jdbcTemplate.queryForObject(anyString(), any(Object[].class), eq(Long.class))).thenReturn(null);

        Long result = repository.getTotalCount(UUID.randomUUID());

        assertThat(result).isZero();
    }

    @Test
    void getReferCountFallsBackToZeroWhenJdbcReturnsNull() {
        AnalyticsClickHouseRepository repository = new AnalyticsClickHouseRepository(jdbcTemplate);
        when(jdbcTemplate.queryForObject(anyString(), any(Object[].class), eq(Integer.class))).thenReturn(null);

        Integer result = repository.getReferCount(UUID.randomUUID());

        assertThat(result).isZero();
    }
}
