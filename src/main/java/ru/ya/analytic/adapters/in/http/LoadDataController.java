package ru.ya.analytic.adapters.in.http;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.ya.analytic.adapters.in.model.RequestedEvent;
import ru.ya.analytic.adapters.in.model.ReferedEvent;
import ru.ya.analytic.application.in.LoadDataUseCase;

import java.util.List;

@RestController
@RequestMapping("/api/load")
@RequiredArgsConstructor
public class LoadDataController {

    private final LoadDataUseCase loadDataUseCase;

    @PostMapping("/requested")
    public boolean loadRequested(@RequestBody List<RequestedEvent> events) {
        return loadDataUseCase.loadRequested(events);
    }

    @PostMapping("/referred")
    public boolean loadReferred(@RequestBody List<ReferedEvent> events) {
        return loadDataUseCase.loadReferred(events);
    }
}