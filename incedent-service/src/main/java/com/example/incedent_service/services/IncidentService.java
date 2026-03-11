package com.example.incedent_service.services;

import com.example.common.events.IncidentPriority;
import com.example.common.events.IncidentStatus;
import com.example.common.events.*;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.incedent_service.repositories.*;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

@Service
@Slf4j
public class IncidentService {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final IncidentRepository incidentRepository;
    private final MeterRegistry meterRegistry;

    private final Counter kafkaIncidentsCreated;
    private final Counter kafkaIncidentsUpdated;
    private final Counter kafkaIncidentsFound;
    private final Counter kafkaProcessingErrors;
    private final Timer kafkaProcessingTimer;

    private static final String INCIDENT_CREATE_TOPIC = "incident-create";
    private static final String INCIDENT_RESPONSE_TOPIC = "incident-response";
    private static final String INCIDENT_UPDATE_TOPIC = "incident-update";
    private static final String INCIDENT_FIND_REQUEST_TOPIC = "incident-find-request";
    private static final String INCIDENT_FIND_RESPONSE_TOPIC = "incident-find-response";
    private static final String INCIDENT_HIGH_PRIORITY_ALERT_TOPIC = "high-priority-alert";

    public IncidentService(
            KafkaTemplate<String, Object> kafkaTemplate,
            IncidentRepository incidentRepository,
            MeterRegistry meterRegistry) {

        this.kafkaTemplate = kafkaTemplate;
        this.incidentRepository = incidentRepository;
        this.meterRegistry = meterRegistry;

        kafkaIncidentsCreated = Counter.builder("incidents.kafka.created.total")
                .description("Incident Create counter")
                .tag("source", "kafka")
                .register(meterRegistry);

        kafkaIncidentsUpdated = Counter.builder("incidents.kafka.updated.total")
                .description("Status update counter")
                .tag("source", "kafka")
                .register(meterRegistry);


        kafkaProcessingErrors = Counter.builder("incidents.kafka.errors.total")
                .description("Errors kafka")
                .tag("source", "kafka")
                .register(meterRegistry);

        kafkaProcessingTimer = Timer.builder("incidents.kafka.processing.time")
                .description("Processing kafka timer")
                .tag("source", "kafka")
                .register(meterRegistry);

        kafkaIncidentsFound = Counter.builder("incidents.kafka.found.total")
                .description("Произвдеенных поисков")
                .tag("source", "kafka")
                .register(meterRegistry);
    }

    @KafkaListener(topics = INCIDENT_CREATE_TOPIC, groupId = "incident-service-group")
    @Transactional
    public void createIncident(ConsumerRecord<String, IncidentCreateRequest> record) {
        Timer.Sample timer = Timer.start();
        String uuid = record.key();

        try {
            log.info("Create incident: " + record);
           IncidentCreateRequest request = record.value();
           Incident incident = Incident.newBuilder()
                    .setService(request.getService())
                    .setInfo(request.getInfo())
                    .setPriority(IncidentPriority.valueOf(request.getPriority().name()))
                    .setStatus(IncidentStatus.CREATED)
                    .build();

            incidentRepository.save(incident);
            kafkaIncidentsCreated.increment();

            log.info("Incident created from Kafka. Id +" + incident.getId());


            com.example.common.events.IncidentCreateResponse response = com.example.common.events.IncidentCreateResponse.newBuilder()
                    .setId(incident.getId())
                    .setService(incident.getService())
                    .setInfo(incident.getInfo())
                    .setStatus(IncidentStatus.valueOf(incident.getStatus().name()))
                    .setPriority(IncidentPriority.valueOf(incident.getPriority().name()))
                    .setTimestamp(incident.getTimestamp() * 1000)
                    .build();

            kafkaTemplate.send(INCIDENT_RESPONSE_TOPIC, uuid, response)
                    .whenComplete((result,ex) -> {
                        if(ex == null)
                        {
                            log.info("Ответ отправлен" + incident);
                        }else{
                            log.error("Ошибка при отправке ответа");
                        }
                    });
            if (incident.getPriority() == IncidentPriority.HIGH) {
                com.example.common.events.Incident highPriorityIncident = mapToAvroIncident(incident);
                kafkaTemplate.send(INCIDENT_HIGH_PRIORITY_ALERT_TOPIC, uuid, highPriorityIncident);
                log.info("Отправлено оповещение о HIGH PRIORITY. id: " + incident.getId());
            }

        } catch (Exception e) {
            kafkaProcessingErrors.increment();
            log.error("Ошибка при создании инцидента "+ e.getMessage());
            throw new RuntimeException("Ошибка при создании инцидента" + e);
        } finally {
            timer.stop(kafkaProcessingTimer);
        }
    }

    @KafkaListener(topics = INCIDENT_UPDATE_TOPIC, groupId = "incident-service-group")
    @Transactional
    public void updateIncident(ConsumerRecord<String, IncidentUpdateRequest> record) {
        Timer.Sample timer = Timer.start();
        String uuid = record.key();
        try {
            log.info("Update status id: " + record.value().getId());
            IncidentUpdateRequest request = record.value();
            Incident incident = incidentRepository.findById(request.getId())
                    .orElseThrow(() -> new RuntimeException("Incident not found by id: " + request.getId()));

            boolean isUpdated = false;

            if (request.getService() != null && !request.getService().equals(incident.getService())) {
                incident.setService(request.getService());
                isUpdated = true;
            }

            if (request.getStatus() != null) {
                incident.setStatus(IncidentStatus.valueOf(request.getStatus().toString()));
                isUpdated = true;
            }

            if (request.getPriority() != null) {
                IncidentPriority newPriority = IncidentPriority.valueOf(request.getPriority().toString());
                incident.setPriority(newPriority);
                isUpdated = true;
            }

            if(isUpdated){
                Incident updatedIncident = incidentRepository.save(incident);
                kafkaIncidentsUpdated.increment();
                log.info("Инцидент обновлен? id: " + updatedIncident.getId());

                com.example.common.events.IncidentUpdateResponse response = com.example.common.events.IncidentUpdateResponse.newBuilder()
                        .setId(updatedIncident.getId())
                        .setService(updatedIncident.getService())
                        .setInfo(updatedIncident.getInfo())
                        .setStatus(com.example.common.events.IncidentStatus.valueOf(updatedIncident.getStatus().name()))
                        .setPriority(com.example.common.events.IncidentPriority.valueOf(updatedIncident.getPriority().name()))
                        .setUpdatedAt(updatedIncident.getTimestamp() * 1000)
                        .setTimestamp(Instant.now().toEpochMilli())
                        .build();

                kafkaTemplate.send(INCIDENT_RESPONSE_TOPIC, uuid, response);
            }


        } catch (Exception e) {
            kafkaProcessingErrors.increment();
            log.error("Error processing update :" + e.getMessage());
            throw e;
        } finally {
            timer.stop(kafkaProcessingTimer);
        }
    }

    @KafkaListener(topics = INCIDENT_FIND_REQUEST_TOPIC, groupId = "incident-service-group")
    @Transactional(readOnly = true)
    public void findIncidents(ConsumerRecord<String, com.example.common.events.IncidentFindRequest> record) {
        Timer.Sample timer = Timer.start();
        String uuid = record.key();

        try {
            log.info("Поиск инцидента");
            com.example.common.events.IncidentFindRequest request = record.value();
            Specification<Incident> spec = buildSpecification(request);
            List<Incident> incidentList = incidentRepository.findAll(spec);

            List<com.example.common.events.Incident> avroIncidents = incidentList.stream()
                    .map(this::mapToAvroIncident)
                    .toList();
            com.example.common.events.IncidentFindResponse response = com.example.common.events.IncidentFindResponse.newBuilder()
                    .setIncidents(avroIncidents)
                    .build();

            kafkaTemplate.send(INCIDENT_FIND_RESPONSE_TOPIC, uuid, response)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Результат поиска отправлен");
                        } else {
                            log.error("Ошибки при поиске");
                        }
                    });
            kafkaIncidentsFound.increment();
        } catch (Exception e) {
            kafkaProcessingErrors.increment();
            log.error("Ошибка при поиске: " + e.getMessage());
            com.example.common.events.IncidentFindResponse errorResponse = com.example.common.events.IncidentFindResponse.newBuilder()
                    .setIncidents(new ArrayList<>())
                    .build();
            kafkaTemplate.send(INCIDENT_FIND_RESPONSE_TOPIC, uuid, errorResponse);
        } finally {
            timer.stop(kafkaProcessingTimer);
        }
    }

    private Specification<Incident> buildSpecification(com.example.common.events.IncidentFindRequest request) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (request.getId() != null) {
                predicates.add(cb.equal(root.get("id"), request.getId()));
            }

            if (request.getService() != null && !request.getService().isEmpty()) {
                predicates.add(cb.equal(root.get("service"), request.getService()));
            }

            if (request.getPriority() != null) {
                predicates.add(cb.equal(root.get("priority"),
                        IncidentPriority.valueOf(request.getPriority().toString())));
            }

            if (request.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"),
                        IncidentStatus.valueOf(request.getStatus().toString())));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    ///  из Incident (сущность в entities) в avro
    private IncidentStatus convertStatus(IncidentStatus status) {
        return IncidentStatus.valueOf(status.name());
    }

    private IncidentPriority convertPriority(IncidentPriority priority) {
        return IncidentPriority.valueOf(priority.name());
    }

    private com.example.common.events.Incident mapToAvroIncident(Incident incident) {
        return com.example.common.events.Incident.newBuilder()
                .setId(incident.getId())
                .setService(incident.getService())
                .setInfo(incident.getInfo())
                .setStatus(com.example.common.events.IncidentStatus.valueOf(incident.getStatus().name()))
                .setPriority(com.example.common.events.IncidentPriority.valueOf(incident.getPriority().name()))
                .setTimestamp(incident.getTimestamp() * 1000)
                .build();
    }
}
