package com.example.incedent_producer_service.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentUpdateResponse {
    private Long id;
    private String service;
    private String info;
    private Incident.IncidentStatus status;
    private Incident.IncidentPriority priority;
    private LocalDateTime updatedAt;
}
