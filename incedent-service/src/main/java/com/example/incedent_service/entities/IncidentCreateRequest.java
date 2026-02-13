package com.example.incedent_service.entities;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentCreateRequest {
    @NotBlank
    private String service;

    @NotBlank
    private String info;

    @NotNull
    private Incident.IncidentPriority priority;
}

