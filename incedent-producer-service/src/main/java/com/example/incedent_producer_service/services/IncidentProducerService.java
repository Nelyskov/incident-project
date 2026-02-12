package com.example.incedent_producer_service.services;

import com.example.common.events.IncidentCreatedEvent;
import com.example.common.events.IncidentPriority;
import com.example.incedent_producer_service.config.KafkaConfig;
import com.example.incedent_producer_service.entities.CreateIncidentRequest;
import com.example.incedent_producer_service.entities.Incident;
import com.example.incedent_producer_service.entities.IncidentFindRequest;
import com.example.incedent_producer_service.entities.IncidentFindResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class IncidentProducerService {

    private  final KafkaTemplate<String, Object> kafkaTemplate;
    private final ConcurrentHashMap<String, CompletableFuture<IncidentCreatedEvent>> pendingCreateRequests = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CompletableFuture<IncidentFindResponse>> pendingFindRequests = new ConcurrentHashMap<>();
    private static final String INCIDENT_CREATE_TOPIC = "incident-create";
    private static final String INCIDENT_RESPONSE_TOPIC = "incident-response";
    private static final String INCIDENT_UPDATE_STATUS_TOPIC = "incident-status-update";
    private static final String INCIDENT_UPDATE_PRIORITY_TOPIC = "incident-priority-update";
    private static final String INCIDENT_FIND_TOPIC = "incident-find";
    private static final String INCIDENT_HIGH_PRIORITY_ALERT = "high-priority-alert";

    public IncidentCreatedEvent createIncident(CreateIncidentRequest request) throws ExecutionException, InterruptedException, TimeoutException {
        String uuid = java.util.UUID.randomUUID().toString();
        CompletableFuture<IncidentCreatedEvent> future = new CompletableFuture<>();
        pendingCreateRequests.put(uuid, future);

        IncidentCreatedEvent event = IncidentCreatedEvent.newBuilder()
                .setId(0l)
                .setService(request.getService())
                .setInfo(request.getInfo())
                .setService(request.getService())
                .setPriority(request.getPriority())
                .setTimestamp(Instant.now().toEpochMilli())
                .build();
        kafkaTemplate.send(INCIDENT_CREATE_TOPIC, uuid, event)
                .whenComplete((result, ex) -> {
                    if(ex == null)
                    {
                        log.info("Событие отправлено");
                    }
                    else {
                        log.error("Ошибка при отправке");
                        future.completeExceptionally(ex);
                        pendingCreateRequests.remove(uuid);
                    }
                });

        IncidentCreatedEvent incidentCreated = future.get(30, TimeUnit.SECONDS);
        pendingCreateRequests.remove(uuid);
        log.info("Инцидент создан в бд");
        if (incidentCreated.getPriority() == IncidentPriority.HIGH) {
            kafkaTemplate.send(INCIDENT_HIGH_PRIORITY_ALERT, incidentCreated);
            log.info("Высокий приоритет");
        }

        return incidentCreated;
    }

    public IncidentFindResponse findIncidents(IncidentFindRequest request) throws ExecutionException, InterruptedException, TimeoutException {
        String uuid = java.util.UUID.randomUUID().toString();
        CompletableFuture<IncidentFindResponse> future = new CompletableFuture<>();
        pendingFindRequests.put(uuid, future);

        kafkaTemplate.send(INCIDENT_FIND_TOPIC, uuid, request)
                .whenComplete((result, ex) -> {
                    if(ex == null){
                        log.info("Событие отправлено");
                    }
                    else {
                        log.error("Ошибка при отправке");
                        future.completeExceptionally(ex);
                        pendingCreateRequests.remove(uuid);
                    }
                });
        IncidentFindResponse response = future.get(30, TimeUnit.SECONDS);
        pendingCreateRequests.remove(uuid);
        log.info("Событие получено");
        return response;
    }

}
