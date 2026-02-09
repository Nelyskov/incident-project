package com.example.incedent_service.repoositories;

import com.example.incedent_service.entities.Incident;
import com.example.incedent_service.entities.IncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {
    Optional<Incident> findById(Long id);
    List<Incident> findByStatus(IncidentStatus status);
    List<Incident> findByService(String service);
    List<Incident> findByStatusAndService(IncidentStatus status, String service);
}
