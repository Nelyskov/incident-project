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

public class Incident {
    private Long id;
    private String service;
    private String info;
    private LocalDateTime createdAt;
    private IncidentStatus status;
    private IncidentPriority priority;
    private LocalDateTime updatedAt;
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = IncidentStatus.CREATED;
        }
    }

    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum IncidentStatus{
        CREATED,
        PROCESSING,
        COMPLETED,
        CANCELED
    }

    public enum IncidentPriority {
        HIGH,
        MEDIUM,
        LOW
    }
}
