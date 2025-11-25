package ru.ya.analytic.application.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.ya.analytic.adapters.in.model.ReferedEvent;
import ru.ya.analytic.adapters.in.model.RequestedEvent;
import ru.ya.analytic.application.in.LoadDataUseCase;
import ru.ya.analytic.application.out.LoadDataPort;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LoadDataService implements LoadDataUseCase {
    private final LoadDataPort adapter;

    @Override
    public boolean loadRequested(List<RequestedEvent> events) {
        return adapter.loadRequested(events);

    }

    @Override
    public boolean loadReferred(List<ReferedEvent> events) {
        return adapter.loadReferred(events);
    }
}
