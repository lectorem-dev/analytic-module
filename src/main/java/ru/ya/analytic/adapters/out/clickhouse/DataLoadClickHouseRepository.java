package ru.ya.analytic.adapters.out.clickhouse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.ya.analytic.adapters.in.model.ReferedEvent;
import ru.ya.analytic.adapters.in.model.RequestedEvent;
import ru.ya.analytic.application.out.LoadDataPort;

import java.sql.Date;
import java.util.List;

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
    public boolean loadRequested(List<RequestedEvent> events) {
        log.debug("Saving {} requested events", events.size());

        clickHouseJdbcTemplate.batchUpdate(
                SQL_INSERT_REQUESTED,
                events,
                1000,
                (ps, event) -> {
                    ps.setDate(1, Date.valueOf(event.getEventDate()));
                    ps.setString(2, event.getCid().toString());
                    ps.setString(3, event.getMid().toString());
                    ps.setInt(4, event.getCount());
                }
        );

        return true;
    }

    @Override
    public boolean loadReferred(List<ReferedEvent> events) {
        log.debug("Saving {} referred events", events.size());

        clickHouseJdbcTemplate.batchUpdate(
                SQL_INSERT_REFERED,
                events,
                1000,
                (ps, event) -> {
                    ps.setDate(1, Date.valueOf(event.getEventDate()));
                    ps.setString(2, event.getMid().toString());
                    ps.setInt(3, event.getCount());
                }
        );

        return true;
    }
}