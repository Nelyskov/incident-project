package com.example.incedent_producer_service.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentFindRequest {
    private Long id;
    private String service;
    private Incident.IncidentPriority priority;
    private Incident.IncidentStatus status;
}