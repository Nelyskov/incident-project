package com.example.incedent_service.entities;

import com.example.common.events.IncidentPriority;
import com.example.common.events.IncidentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentResponse {
    private Long id;
    private String service;
    private String info;
    private LocalDateTime createdAt;
    private IncidentStatus status;
    private IncidentPriority priority;
    private LocalDateTime updatedAt;
}
