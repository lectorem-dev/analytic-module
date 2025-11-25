package ru.ya.analytic.adapters.in.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReferedEvent {
    private UUID mid;
    private Integer count;
    private LocalDate eventDate;
}