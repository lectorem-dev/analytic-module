package ru.ya.analytic.application.in;

import ru.ya.libs.model.ReferedEvent;
import ru.ya.libs.model.RequestedEvent;

public interface LoadDataUseCase {
    boolean loadRequested(RequestedEvent events);
    boolean loadReferred(ReferedEvent events);
}

