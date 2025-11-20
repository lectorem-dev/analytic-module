package ru.ya.analytic.application.out;

import ru.ya.analytic.adapters.in.model.ReferedEvent;
import ru.ya.analytic.adapters.in.model.RequestedEvent;

import java.util.List;

public interface LoadDataPort {
    boolean loadRequested(List<RequestedEvent> events);

    boolean loadReferred(List<ReferedEvent> events);
}
