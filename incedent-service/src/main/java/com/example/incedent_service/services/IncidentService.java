package com.example.incedent_service.services;

import com.example.incedent_service.entities.*;
import com.example.incedent_service.repoositories.IncidentRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class IncidentService {
    private final KafkaTemplate<String, Incident> kafkaTemplate;
    private final IncidentRepository incidentRepository;

    private static final String INCIDENT_CREATED_TOPIC = "incident-created";
    private static final String INCIDENT_UPDATE_TOPIC = "incident-updated";

    @Transactional
    public IncidentResponse createIncident(CreateIncidentRequest request){
        Incident incident = Incident.builder()
                .service(request.getService())
                .info(request.getInfo())
                .priority(request.getPriority())
                .status(IncidentStatus.CREATED)
                .build();
        incidentRepository.save(incident);

        sendKafkaEvent(INCIDENT_UPDATE_TOPIC, incident, "created incident");
        return mapToResponse(incident);
    }

    @Transactional
    public IncidentResponse updateIncident(Long id, UpdateIncidentStatusRequest request){
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Incident not foubd by id: " + id));
        incident.setStatus(request.getStatus());
        incidentRepository.save(incident);
        sendKafkaEvent(INCIDENT_CREATED_TOPIC, incident, "updated" );

        log.info("Incident status updated to: " + incident.getStatus());

        return mapToResponse(incident);
    }

    private void sendKafkaEvent(String topic, Incident incident, String eventType){
        kafkaTemplate.send(topic, incident)
                .whenComplete((result, ex) -> {
                    if(ex == null)
                        log.info("Incident event send to kafka. IncidentId:" + incident.getId());
                    else {
                        log.info("Incident event dont send to kafka. IncidentId:" + incident.getId());
                    }
                });
    }

    public IncidentResponse getIncidentById(long id){
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Incident not foubd by id: " + id));
        return mapToResponse(incident);
    }

    public List<IncidentResponse> getAllIncidents(){
        return incidentRepository.findAll()
                .stream().map(i -> mapToResponse(i)).toList();
    }

    private IncidentResponse mapToResponse(Incident incident){
        return IncidentResponse.builder()
                .id(incident.getId())
                .service(incident.getService())
                .info(incident.getInfo())
                .status(incident.getStatus())
                .createdAt(incident.getCreatedAt())
                .updatedAt(incident.getUpdatedAt())
                .build();
    }



}
