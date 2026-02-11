package com.example.incedent_service.services;

import com.example.common.events.IncidentCreateRequest;
import com.example.common.events.IncidentPriority;
import com.example.common.events.IncidentStatus;
import com.example.common.events.IncidentUpdateRequest;
import com.example.incedent_service.entities.Incident;
import com.example.incedent_service.entities.IncidentFindRequest;
import com.example.common.events.IncidentFindResponse;
import com.example.incedent_service.entities.IncidentResponse;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.incedent_service.repositories.*;

import java.sql.Time;
import java.util.List;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class IncidentService {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private final IncidentRepository incidentRepository;

    private final Counter kafkaIncidentsCreated;
    private final Counter kafkaIncidentsUpdatedStatus;
    private final Counter kafkaIncidentsUpdatedPriority;
    private final Counter kafkaIncidentsFound;
    private final Counter kafkaProcessingErrors;
    private final Timer kafkaProcessingTimer;

    private static final String INCIDENT_CREATE_TOPIC = "incident-create";
    private static final String INCIDENT_UPDATE_STATUS_TOPIC = "incident-status-update";
    private static final String INCIDENT_UPDATE_PRIORITY_TOPIC = "incident-priority-update";
    private static final String INCIDENT_FIND_REQUEST_TOPIC = "incident-find-request";

    public IncidentService(
            KafkaTemplate<String, Object> kafkaTemplate,
            IncidentRepository incidentRepository,
            MeterRegistry meterRegistry) {

        this.kafkaTemplate = kafkaTemplate;
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

        kafkaIncidentsFound = Counter.builder("incidents.kafka.found.total")
                .description("Произвдеенных поисков")
                .tag("source", "kafka")
                .register(meterRegistry);
    }

    @KafkaListener(topics = INCIDENT_CREATE_TOPIC, groupId = "service-group")
    @Transactional
    public void createIncident(IncidentCreateRequest request) {
        Timer.Sample timer = Timer.start();

        try {
            log.info("Create incident: " + request);

           Incident incident = Incident.builder()
                    .service(request.getService())
                    .info(request.getInfo())
                    .priority(Incident.IncidentPriority.valueOf(request.getPriority().name()))
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
    public void updateIncidentStatus(IncidentUpdateRequest request) {
        Timer.Sample timer = Timer.start();

        try {
            log.info("Update status id: " + request.getId());

            Incident incident = incidentRepository.findById(request.getId())
                    .orElseThrow(() -> new RuntimeException("Incident not found by id: " + request.getId()));

            incident.setStatus(Incident.IncidentStatus.valueOf(request.getStatus().toString()));
            incidentRepository.save(incident);
            kafkaIncidentsUpdatedStatus.increment();

            log.info("Incident status updated to: " + request.getStatus());

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
    public void updateIncidentPriority(IncidentUpdateRequest request) {
        Timer.Sample timer = Timer.start();

        try {
            log.info("Update priority: " + request.getId());

            Incident incident = incidentRepository.findById(request.getId())
                    .orElseThrow(() -> new RuntimeException("Incident not found by id: " + request.getId()));

            incident.setPriority(Incident.IncidentPriority.valueOf(request.getPriority().toString()));
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

    ///  из Incident (сущность в entities) в avro
    private IncidentStatus convertStatus(Incident.IncidentStatus status) {
        return IncidentStatus.valueOf(status.name());
    }

    private IncidentPriority convertPriority(Incident.IncidentPriority priority) {
        return IncidentPriority.valueOf(priority.name());
    }

    private IncidentResponse mapToResponse(Incident incident) {
        return IncidentResponse.builder()
                .id(incident.getId())
                .service(incident.getService())
                .info(incident.getInfo())
                .status(com.example.common.events.IncidentStatus.valueOf(incident.getStatus().name()))
                .priority(com.example.common.events.IncidentPriority.valueOf(incident.getPriority().name()))
                .createdAt(incident.getCreatedAt())
                .updatedAt(incident.getUpdatedAt())
                .build();
    }

    private com.example.common.events.Incident mapToAvroIncident(Incident incident) {
        return com.example.common.events.Incident.newBuilder()
                .setId(incident.getId())
                .setService(incident.getService())
                .setInfo(incident.getInfo())
                .setStatus(com.example.common.events.IncidentStatus.valueOf(incident.getStatus().name()))
                .setPriority(com.example.common.events.IncidentPriority.valueOf(incident.getPriority().name()))
                .setTimestamp(incident.getUpdatedAt() != null ?
                        incident.getUpdatedAt().toEpochSecond(java.time.ZoneOffset.UTC) * 1000 :
                        incident.getCreatedAt().toEpochSecond(java.time.ZoneOffset.UTC) * 1000)
                .build();
    }

    @KafkaListener(topics = INCIDENT_FIND_REQUEST_TOPIC, groupId = "service-group")
    public void findIncidents(IncidentFindRequest request){
        Timer.Sample timer = Timer.start();
        try {
            log.info("Запрос на поиск инцидентов: {}", request);

            List<com.example.common.events.Incident> avroIncidents = new ArrayList<>();

            if (request.getId() != null) {
                incidentRepository.findById(request.getId())
                        .map(this::mapToAvroIncident)
                        .ifPresent(avroIncidents::add);
            } else {
                avroIncidents = incidentRepository.findAll()
                        .stream()
                        .map(this::mapToAvroIncident)
                        .toList();
            }

            IncidentFindResponse response = IncidentFindResponse.newBuilder()
                    .setIncidents(avroIncidents)
                    .build();

            kafkaTemplate.send("incident-find-response", response);
            kafkaIncidentsFound.increment();

            log.info("Найдено инцидентов" + avroIncidents.size());

        } catch (Exception e) {
            kafkaProcessingErrors.increment();
            log.error("Ошибка при поиске инцидентов: " + e.getMessage());

            IncidentFindResponse errorResponse = IncidentFindResponse.newBuilder()
                    .setIncidents(new ArrayList<>())
                    .build();
            kafkaTemplate.send("incident-find-response", errorResponse);
        } finally {
            timer.stop(kafkaProcessingTimer);
        }
    }
}