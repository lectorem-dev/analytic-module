package ru.ya.analytic.application.out;

import ru.ya.libs.model.ReferedEvent;
import ru.ya.libs.model.RequestedEvent;

public interface LoadDataPort {
    boolean loadRequested(RequestedEvent events);
    boolean loadReferred(ReferedEvent events);
}