package ru.ya.libs.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestedEvent {
    private UUID categoryId;
    private UUID manufactureId;
    private Integer count;
    private LocalDate eventDate;
}
