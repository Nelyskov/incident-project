package com.example.incedent_service.services;


import com.example.common.events.IncidentCreateRequest;
import com.example.common.events.IncidentCreateResponse;
import com.example.common.events.IncidentUpdateRequest;
import com.example.common.events.IncidentUpdateResponse;
import com.example.common.events.IncidentFindRequest;
import com.example.common.events.IncidentFindResponse;

import com.example.incedent_service.entities.Incident;
import com.example.incedent_service.entities.IncidentStatus;
import com.example.incedent_service.entities.IncidentPriority;
import com.example.incedent_service.repositories.IncidentRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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
    private static final String INCIDENT_CREATE_RESPONSE_TOPIC = "incident-create-response"; // FIX
    private static final String INCIDENT_UPDATE_RESPONSE_TOPIC = "incident-update-response"; // FIX
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
                .description("Поиск инцидентов")
                .tag("source", "kafka")
                .register(meterRegistry);
    }

    @KafkaListener(topics = INCIDENT_CREATE_TOPIC,
                   groupId = "incident-service-group",
                   containerFactory = "incidentServiceConsumerKafkaTemplate")
    @Transactional
    public void createIncident(ConsumerRecord<String, IncidentCreateRequest> record, Acknowledgment ack) {
        Timer.Sample timer = Timer.start(meterRegistry);
        String uuid = record.key();

        try {
            log.info("Create incident: {}", record);
            IncidentCreateRequest request = record.value();
            Incident incident = Incident.builder()
                    .service(request.getService().toString())
                    .info(request.getInfo().toString())
                    .priority(IncidentPriority.valueOf(request.getPriority().name()))
                    .status(IncidentStatus.CREATED)
                    .build();

            incidentRepository.save(incident);
            kafkaIncidentsCreated.increment();

            log.info("Incident created from Kafka. Id: {}", incident.getId());

            IncidentCreateResponse response = IncidentCreateResponse.newBuilder()
                    .setId(incident.getId())
                    .setService(incident.getService())
                    .setInfo(incident.getInfo())
                    .setStatus(com.example.common.events.IncidentStatus
                            .valueOf(incident.getStatus().name()))
                    .setPriority(com.example.common.events.IncidentPriority
                            .valueOf(incident.getPriority().name()))
                    .setTimestamp(incident.getTimestamp())
                    .build();

            kafkaTemplate.send(INCIDENT_CREATE_RESPONSE_TOPIC, uuid, response)
                    .whenComplete((result,ex) -> {
                        if(ex == null)
                        {
                            log.info("Ответ отправлен" + incident.getId());
                        }else{
                            log.error("Ошибка при отправке ответа", ex);
                        }
                    });
            if (incident.getPriority() == IncidentPriority.HIGH) {
                com.example.common.events.Incident highPriorityIncident = mapToAvroIncident(incident);
                kafkaTemplate.send(INCIDENT_HIGH_PRIORITY_ALERT_TOPIC, uuid, highPriorityIncident);
                log.info("Отправлено оповещение HIGH PRIORITY. id: {}", incident.getId());
            }

        } catch (Exception e) {
            kafkaProcessingErrors.increment();
            log.error("Ошибка при создании инцидента: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка при создании инцидента", e);
        } finally {
            timer.stop(kafkaProcessingTimer);
        }
    }

    @KafkaListener(
            topics = INCIDENT_UPDATE_TOPIC,
            groupId = "incident-service-group",
            containerFactory = "incidentServiceConsumerKafkaTemplate")
    @Transactional
    public void updateIncident(ConsumerRecord<String, IncidentUpdateRequest> record, Acknowledgment ack) {
        Timer.Sample timer = Timer.start(meterRegistry); // FIX
        String uuid = record.key();
        try {
            log.info("Update status id: {}", record.value().getId()); // FIX: {}
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
                incident.setPriority(IncidentPriority.valueOf(request.getPriority().toString()));
                isUpdated = true;
            }

            if (isUpdated) {
                Incident updatedIncident = incidentRepository.save(incident);
                kafkaIncidentsUpdated.increment();
                log.info("Инцидент обновлён. id: {}", updatedIncident.getId());

                IncidentUpdateResponse response = IncidentUpdateResponse.newBuilder()
                        .setId(updatedIncident.getId())
                        .setService(updatedIncident.getService())
                        .setInfo(updatedIncident.getInfo())
                        .setStatus(com.example.common.events.IncidentStatus
                                .valueOf(updatedIncident.getStatus().name()))
                        .setPriority(com.example.common.events.IncidentPriority
                                .valueOf(updatedIncident.getPriority().name()))
                        .setUpdatedAt(updatedIncident.getTimestamp())
                        .setTimestamp(Instant.now().toEpochMilli())
                        .build();

                kafkaTemplate.send(INCIDENT_UPDATE_RESPONSE_TOPIC, uuid, response);
            }

            ack.acknowledge();

        } catch (Exception e) {
            kafkaProcessingErrors.increment();
            log.error("Error processing update: {}", e.getMessage(), e);
            throw e;
        } finally {
            timer.stop(kafkaProcessingTimer);
        }
    }


    @KafkaListener(
            topics = INCIDENT_FIND_REQUEST_TOPIC,
            groupId = "incident-service-group",
            containerFactory = "incidentServiceConsumerKafkaTemplate" )
    @Transactional(readOnly = true)
    public void findIncidents(
            ConsumerRecord<String, com.example.common.events.IncidentFindRequest> record,
            Acknowledgment ack) { //

        Timer.Sample timer = Timer.start(meterRegistry);
        String uuid = record.key();

        try {
            log.info("Поиск инцидента. uuid: {}", uuid);
            IncidentFindRequest request = record.value();

            List<com.example.common.events.Incident> avroList = incidentRepository
                    .findAll(buildSpecification(request))
                    .stream()
                    .map(this::mapToAvroIncident)
                    .toList();

            IncidentFindResponse response = IncidentFindResponse.newBuilder()
                    .setIncidents(avroList)
                    .build();

            kafkaTemplate.send(INCIDENT_FIND_RESPONSE_TOPIC, uuid, response)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Результат поиска отправлен. uuid: {}", uuid);
                        } else {
                            log.error("Ошибка при отправке результата поиска", ex);
                        }
                    });

            kafkaIncidentsFound.increment();
            ack.acknowledge();

        } catch (Exception e) {
            kafkaProcessingErrors.increment();
            log.error("Ошибка при поиске: {}", e.getMessage(), e);
            com.example.common.events.IncidentFindResponse errorResponse =
                    com.example.common.events.IncidentFindResponse.newBuilder()
                            .setIncidents(new ArrayList<>())
                            .build();
            kafkaTemplate.send(INCIDENT_FIND_RESPONSE_TOPIC, uuid, errorResponse);
            ack.acknowledge();
        } finally {
            timer.stop(kafkaProcessingTimer);
        }
    }

    private Specification<Incident> buildSpecification(
            com.example.common.events.IncidentFindRequest request) {
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

    private com.example.common.events.Incident mapToAvroIncident(Incident incident) {
        return com.example.common.events.Incident.newBuilder()
                .setId(incident.getId())
                .setService(incident.getService())
                .setInfo(incident.getInfo())
                .setStatus(com.example.common.events.IncidentStatus.valueOf(
                        incident.getStatus().name()))
                .setPriority(com.example.common.events.IncidentPriority.valueOf(
                        incident.getPriority().name()))
                .setTimestamp(incident.getTimestamp() * 1000)
                .build();
    }
}
