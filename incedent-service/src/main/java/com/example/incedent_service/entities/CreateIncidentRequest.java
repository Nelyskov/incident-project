package com.example.incedent_service.entities;

import com.example.common.events.IncidentPriority;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateIncidentRequest {
    @NotBlank
    private String service;

    @NotBlank
    private String info;

    private IncidentPriority priority;
}

