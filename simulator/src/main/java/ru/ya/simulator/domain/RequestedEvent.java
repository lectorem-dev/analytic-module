package ru.ya.simulator.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestedEvent {
    private UUID cid;
    private UUID mid;
    private Integer count;
    private LocalDate eventDate;
}
