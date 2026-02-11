package com.example.incedent_service.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.incedent_service.entities.*;

import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {
    Optional<Incident> findById(Long id);
    List<Incident> findByStatus(Incident.IncidentStatus status);
    List<Incident> findByService(String service);
    List<Incident> findByPriority(Incident.IncidentPriority priority);
}
