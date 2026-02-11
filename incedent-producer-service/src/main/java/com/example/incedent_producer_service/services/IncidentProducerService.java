package com.example.incedent_producer_service.services;

import com.example.common.events.IncidentCreatedEvent;
import com.example.common.events.IncidentPriority;
import com.example.incedent_producer_service.config.KafkaConfig;
import com.example.incedent_producer_service.entities.CreateIncidentRequest;
import com.example.incedent_producer_service.entities.Incident;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class IncidentProducerService {

    private  final KafkaTemplate<String, Object> kafkaTemplate;
    private final ConcurrentHashMap<String, CompletableFuture<Incident>> pendingRequests = new ConcurrentHashMap<>();
    private static final String INCIDENT_CREATE_TOPIC = "incident-create";
    private static final String INCIDENT_RESPONSE_TOPIC = "incident-response";
    private static final String INCIDENT_UPDATE_STATUS_TOPIC = "incident-status-update";
    private static final String INCIDENT_UPDATE_PRIORITY_TOPIC = "incident-priority-update";
    private static final String INCIDENT_FIND_TOPIC = "incident-find";
    private static final String INCIDENT_HIGH_PRIORITY_ALERT = "high-priority-alert";

    public IncidentCreatedEvent createIncident(CreateIncidentRequest request){
        String uuid = java.util.UUID.randomUUID().toString();
        CompletableFuture<Incident> future = new CompletableFuture<>();
        pendingRequests.put(uuid, future);

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
                        pendingRequests.remove(uuid);
                    }
                });

        IncidentCreatedEvent incidentCreated = (IncidentCreatedEvent) future.get(30, TimeUnit.SECONDS);
        log.info("Инцидент создан в бд");
        return incidentCreated;


        if(event.getPriority() == IncidentPriority.HIGH){
            kafkaTemplate.send(INCIDENT_HIGH_PRIORITY_ALERT, event);
        }

    }


}
