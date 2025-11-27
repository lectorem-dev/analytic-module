package ru.ya.analytic.adapters.out.clickhouse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.ya.analytic.application.out.LoadDataPort;
import ru.ya.libs.model.ReferedEvent;
import ru.ya.libs.model.RequestedEvent;

import java.sql.Date;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoadClickHouseRepository implements LoadDataPort {

    private final JdbcTemplate clickHouseJdbcTemplate;

    private static final String SQL_INSERT_REQUESTED = """
        INSERT INTO analytic.requested (event_date, cid, mid, count)
        VALUES (?, ?, ?, ?)
        """;

    private static final String SQL_INSERT_REFERED = """
        INSERT INTO analytic.refered (event_date, mid, count)
        VALUES (?, ?, ?)
        """;

    @Override
    public boolean loadRequested(RequestedEvent event) {
        log.debug("Saving RequestedEvent: {}", event);

        clickHouseJdbcTemplate.update(
                SQL_INSERT_REQUESTED,
                Date.valueOf(event.getEventDate()),
                event.getCategoryId().toString(),
                event.getManufactureId().toString(),
                event.getCount()
        );

        return true;
    }

    @Override
    public boolean loadReferred(ReferedEvent event) {
        log.debug("Saving ReferedEvent: {}", event);

        clickHouseJdbcTemplate.update(
                SQL_INSERT_REFERED,
                Date.valueOf(event.getEventDate()),
                event.getManufactureId().toString(),
                event.getCount()
        );

        return true;
    }
}
