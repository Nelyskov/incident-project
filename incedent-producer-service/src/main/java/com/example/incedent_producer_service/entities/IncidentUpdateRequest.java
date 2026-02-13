package com.example.incedent_producer_service.entities;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentUpdateRequest {
    @NotNull
    private Long id;

    private String service;

    private String info;

    private Incident.IncidentStatus status;

    private Incident.IncidentPriority priority;
}
