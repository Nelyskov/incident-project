package com.example.incedent_producer_service.entities;

import com.example.common.events.IncidentPriority;
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
    private IncidentPriority priority;
}

