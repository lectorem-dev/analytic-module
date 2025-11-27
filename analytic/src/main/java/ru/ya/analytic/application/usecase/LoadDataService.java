package ru.ya.analytic.application.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.ya.analytic.application.in.LoadDataUseCase;
import ru.ya.analytic.application.out.LoadDataPort;
import ru.ya.libs.model.ReferedEvent;
import ru.ya.libs.model.RequestedEvent;

@Service
@RequiredArgsConstructor
public class LoadDataService implements LoadDataUseCase {
    private final LoadDataPort adapter;

    @Override
    public boolean loadRequested(RequestedEvent events) {
        return adapter.loadRequested(events);

    }

    @Override
    public boolean loadReferred(ReferedEvent events) {
        return adapter.loadReferred(events);
    }
}
