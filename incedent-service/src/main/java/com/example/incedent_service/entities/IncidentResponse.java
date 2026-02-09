package com.example.incedent_service.entities;

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
    private String priority;
    private LocalDateTime updatedAt;
}
