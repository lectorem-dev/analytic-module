package ru.ya.analytic.adapters.out.clickhouse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.ya.libs.model.ReferedEvent;
import ru.ya.libs.model.RequestedEvent;

import java.sql.Date;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DataLoadClickHouseRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    void loadRequestedWritesExpectedValuesToJdbcTemplate() {
        DataLoadClickHouseRepository repository = new DataLoadClickHouseRepository(jdbcTemplate);
        UUID categoryId = UUID.randomUUID();
        UUID manufactureId = UUID.randomUUID();
        LocalDate eventDate = LocalDate.of(2026, 4, 10);
        RequestedEvent event = new RequestedEvent(categoryId, manufactureId, 21, eventDate);

        boolean result = repository.loadRequested(event);

        assertThat(result).isTrue();
        verify(jdbcTemplate).update(
                contains("INSERT INTO analytic.requested"),
                eq(Date.valueOf(eventDate)),
                eq(categoryId.toString()),
                eq(manufactureId.toString()),
                eq(21)
        );
    }

    @Test
    void loadReferredWritesExpectedValuesToJdbcTemplate() {
        DataLoadClickHouseRepository repository = new DataLoadClickHouseRepository(jdbcTemplate);
        UUID manufactureId = UUID.randomUUID();
        LocalDate eventDate = LocalDate.of(2026, 4, 10);
        ReferedEvent event = new ReferedEvent(manufactureId, 7, eventDate);

        boolean result = repository.loadReferred(event);

        assertThat(result).isTrue();
        verify(jdbcTemplate).update(
                contains("INSERT INTO analytic.refered"),
                eq(Date.valueOf(eventDate)),
                eq(manufactureId.toString()),
                eq(7)
        );
    }
}
