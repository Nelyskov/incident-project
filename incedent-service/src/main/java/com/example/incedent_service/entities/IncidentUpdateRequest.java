package com.example.incedent_service.entities;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.antlr.v4.runtime.misc.NotNull;

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

