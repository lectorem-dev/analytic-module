package ru.ya.analytic.application.usecase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.ya.analytic.application.out.LoadDataPort;
import ru.ya.libs.model.ReferedEvent;
import ru.ya.libs.model.RequestedEvent;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoadDataServiceTest {

    @Mock
    private LoadDataPort loadDataPort;

    @InjectMocks
    private LoadDataService loadDataService;

    @Test
    void loadRequestedDelegatesToPort() {
        RequestedEvent event = new RequestedEvent(UUID.randomUUID(), UUID.randomUUID(), 12, LocalDate.now());
        when(loadDataPort.loadRequested(event)).thenReturn(true);

        boolean result = loadDataService.loadRequested(event);

        assertThat(result).isTrue();
        verify(loadDataPort).loadRequested(event);
    }

    @Test
    void loadReferredDelegatesToPort() {
        ReferedEvent event = new ReferedEvent(UUID.randomUUID(), 7, LocalDate.now());
        when(loadDataPort.loadReferred(event)).thenReturn(true);

        boolean result = loadDataService.loadReferred(event);

        assertThat(result).isTrue();
        verify(loadDataPort).loadReferred(event);
    }
}
