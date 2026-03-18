package com.example.incedent_service.repositories;

import com.example.incedent_service.entities.Incident;
import com.example.incedent_service.entities.IncidentStatus;
import com.example.incedent_service.entities.IncidentPriority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long>, JpaSpecificationExecutor<Incident> {
    Optional<Incident> findById(Long id);
    List<Incident> findByStatus(IncidentStatus status);
    List<Incident> findByService(String service);
    List<Incident> findByPriority(IncidentPriority priority);
    List<Incident> findByStatusAndPriority(IncidentStatus status, IncidentPriority priority);
    List<Incident> findByServiceAndStatus(String service, IncidentStatus status);
    List<Incident> findByServiceAndPriority(String service, IncidentPriority priority);
}