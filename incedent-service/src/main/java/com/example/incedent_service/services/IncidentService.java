package com.example.incedent_service.services;

import com.example.common.events.IncidentCreatedEvent;
import com.example.common.events.IncidentUpdatedEvent;
import com.example.common.events.IncidentPriority;
import com.example.common.events.IncidentStatus;
import com.example.incedent_service.entities.Incident;
import com.example.incedent_service.entities.IncidentResponse;
import com.example.incedent_service.repositories.IncidentRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class IncidentService {
    private final KafkaTemplate<String, IncidentCreatedEvent> kafkaCreateTemplate;
    private final KafkaTemplate<String, IncidentUpdatedEvent> kafkaUpdateTemplate;
    private final IncidentRepository incidentRepository;

    private final Counter kafkaIncidentsCreated;
    private final Counter kafkaIncidentsUpdatedStatus;
    private final Counter kafkaIncidentsUpdatedPriority;
    private final Counter kafkaProcessingErrors;
    private final Timer kafkaProcessingTimer;

    private static final String INCIDENT_CREATE_TOPIC = "incident-create";
    private static final String INCIDENT_UPDATE_STATUS_TOPIC = "incident-status-update";
    private static final String INCIDENT_UPDATE_PRIORITY_TOPIC = "incident-priority-update";

    public IncidentService(
            KafkaTemplate<String, IncidentCreatedEvent> kafkaCreateTemplate,
            KafkaTemplate<String, IncidentUpdatedEvent> kafkaUpdateTemplate,
            IncidentRepository incidentRepository,
            MeterRegistry meterRegistry) {

        this.kafkaCreateTemplate = kafkaCreateTemplate;
        this.kafkaUpdateTemplate = kafkaUpdateTemplate;
        this.incidentRepository = incidentRepository;

        this.kafkaIncidentsCreated = Counter.builder("incidents.kafka.created.total")
                .description("Incident Create counter")
                .tag("source", "kafka")
                .register(meterRegistry);

        this.kafkaIncidentsUpdatedStatus = Counter.builder("incidents.kafka.updated.status.total")
                .description("Status update counter")
                .tag("source", "kafka")
                .register(meterRegistry);

        this.kafkaIncidentsUpdatedPriority = Counter.builder("incidents.kafka.updated.priority.total")
                .description("Incident priority update counter")
                .tag("source", "kafka")
                .register(meterRegistry);

        this.kafkaProcessingErrors = Counter.builder("incidents.kafka.errors.total")
                .description("Errors kafka")
                .tag("source", "kafka")
                .register(meterRegistry);

        this.kafkaProcessingTimer = Timer.builder("incidents.kafka.processing.time")
                .description("Processing kafka timer")
                .tag("source", "kafka")
                .register(meterRegistry);
    }

    @KafkaListener(topics = INCIDENT_CREATE_TOPIC, groupId = "service-group")
    @Transactional
    public void createIncident(IncidentCreatedEvent event) {
        Timer.Sample timer = Timer.start();

        try {
            log.info("Create incident: " + event);

            Incident incident = Incident.builder()
                    .service(event.getService())
                    .info(event.getInfo())
                    .priority(Incident.IncidentPriority.valueOf(event.getPriority().name()))
                    .status(Incident.IncidentStatus.CREATED)
                    .build();

            incidentRepository.save(incident);
            kafkaIncidentsCreated.increment();

            log.info("Incident created from Kafka. Id +" + incident.getId());

        } catch (Exception e) {
            kafkaProcessingErrors.increment();
            log.error("Error processing create incident :" + e.getMessage());
            throw e;
        } finally {
            timer.stop(kafkaProcessingTimer);
        }
    }

    @KafkaListener(topics = INCIDENT_UPDATE_STATUS_TOPIC, groupId = "service-group")
    @Transactional
    public void updateIncidentStatus(IncidentUpdatedEvent event) {
        Timer.Sample timer = Timer.start();

        try {
            log.info("Update status id: " + event.getId());

            Incident incident = incidentRepository.findById(event.getId())
                    .orElseThrow(() -> new RuntimeException("Incident not found by id: " + event.getId()));

            incident.setStatus(Incident.IncidentStatus.valueOf(event.getStatus().name()));
            incidentRepository.save(incident);
            kafkaIncidentsUpdatedStatus.increment();

            log.info("Incident status updated to: " + event.getStatus());

        } catch (Exception e) {
            kafkaProcessingErrors.increment();
            log.error("Error processing status update :" + e.getMessage());
            throw e;
        } finally {
            timer.stop(kafkaProcessingTimer);
        }
    }

    @KafkaListener(topics = INCIDENT_UPDATE_PRIORITY_TOPIC, groupId = "service-group")
    @Transactional
    public void updateIncidentPriority(IncidentUpdatedEvent event) {
        Timer.Sample timer = Timer.start();

        try {
            log.info("Update priority: " + event.getId());

            Incident incident = incidentRepository.findById(event.getId())
                    .orElseThrow(() -> new RuntimeException("Incident not found by id: " + event.getId()));

            incident.setPriority(Incident.IncidentPriority.valueOf(event.getPriority().name()));
            incidentRepository.save(incident);
            kafkaIncidentsUpdatedPriority.increment();

            log.info("Incident priority updated to: " + incident.getPriority());

        } catch (Exception e) {
            kafkaProcessingErrors.increment();
            log.error("Error processing priority update :" + e.getMessage());
            throw e;
        } finally {
            timer.stop(kafkaProcessingTimer);
        }
    }

    public IncidentResponse getIncidentById(long id) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Incident not found by id: " + id));
        return mapToResponse(incident);
    }

    public List<IncidentResponse> getAllIncidents() {
        return incidentRepository.findAll()
                .stream().map(this::mapToResponse).toList();
    }

    private IncidentResponse mapToResponse(Incident incident) {
        return IncidentResponse.builder()
                .id(incident.getId())
                .service(incident.getService())
                .info(incident.getInfo())
                .status(convertStatus(incident.getStatus()))
                .priority(convertPriority(incident.getPriority()))
                .createdAt(incident.getCreatedAt())
                .updatedAt(incident.getUpdatedAt())
                .build();
    }

    private IncidentStatus convertStatus(Incident.IncidentStatus status) {
        return IncidentStatus.valueOf(status.name());
    }

    private IncidentPriority convertPriority(Incident.IncidentPriority priority) {
        return IncidentPriority.valueOf(priority.name());
    }
}