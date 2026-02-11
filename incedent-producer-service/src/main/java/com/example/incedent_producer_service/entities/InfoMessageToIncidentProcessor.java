package com.example.incedent_producer_service.entities;

import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class InfoMessageToIncidentProcessor {
    public Incident incident;
    public Incident.IncidentPriority priority;
}
