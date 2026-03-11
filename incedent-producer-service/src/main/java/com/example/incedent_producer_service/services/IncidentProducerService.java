package com.example.incedent_producer_service.services;

import com.example.common.events.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class IncidentProducerService {

    private  final KafkaTemplate<String, Object> kafkaTemplate;

    private final ConcurrentHashMap<String, CompletableFuture<IncidentCreateResponse>> pendingCreateRequests = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CompletableFuture<IncidentFindResponse>> pendingFindRequests = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CompletableFuture<IncidentUpdateResponse>> pendingUpdateRequests = new ConcurrentHashMap<>();

    private static final String INCIDENT_CREATE_TOPIC = "incident-create";
    private static final String INCIDENT_RESPONSE_TOPIC = "incident-response";
    private static final String INCIDENT_UPDATE_TOPIC = "incident-update";
    private static final String INCIDENT_FIND_REQUEST_TOPIC = "incident-find-request";
    private static final String INCIDENT_FIND_RESPONSE_TOPIC = "incident-find-response";
    private static final String INCIDENT_HIGH_PRIORITY_ALERT = "high-priority-alert";

    private static final int REQUEST_TIMEOUT_SECONDS = 30;


    public IncidentCreateResponse createIncident(IncidentCreateRequest request)
            throws ExecutionException, InterruptedException, TimeoutException {
        String uuid = java.util.UUID.randomUUID().toString();

        CompletableFuture<IncidentCreateResponse> future = new CompletableFuture<>();
        pendingCreateRequests.put(uuid, future);

        com.example.common.events.IncidentCreateRequest createRequest = com.example.common.events.IncidentCreateRequest.newBuilder()
                .setService(request.getService())
                .setInfo(request.getInfo())
                .setPriority(com.example.common.events.IncidentPriority.valueOf(request.getPriority().name()))
                .build();

        kafkaTemplate.send(INCIDENT_CREATE_TOPIC, uuid, createRequest)
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

        IncidentCreateResponse response= future.get(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (response.getPriority().toString() == "HIGH") {
            kafkaTemplate.send(INCIDENT_HIGH_PRIORITY_ALERT, uuid, response);
            log.info("Высокий приоритет");

        }
        pendingCreateRequests.remove(uuid);
        return response;
    }

    public IncidentUpdateResponse updateIncident(IncidentUpdateRequest request)
            throws ExecutionException, InterruptedException, TimeoutException {
        String uuid = java.util.UUID.randomUUID().toString();

        CompletableFuture<IncidentUpdateResponse> future = new CompletableFuture<>();
        pendingUpdateRequests.put(uuid, future);

        com.example.common.events.IncidentUpdateRequest updateRequest =  com.example.common.events.IncidentUpdateRequest.newBuilder()
                .setId(request.getId())
                .setService(request.getService())
                .setPriority(request.getPriority() != null ? request.getPriority() : null)
                .setStatus(request.getStatus() != null ? request.getStatus() : null)
                .build();
        kafkaTemplate.send(INCIDENT_UPDATE_TOPIC, uuid, updateRequest)
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
        IncidentUpdateResponse response = future.get(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        return response;
    }

    public IncidentFindResponse findIncidents(IncidentFindRequest request)
            throws ExecutionException, InterruptedException, TimeoutException {

        String uuid = java.util.UUID.randomUUID().toString();
        CompletableFuture<IncidentFindResponse> future = new CompletableFuture<>();
        pendingFindRequests.put(uuid, future);

        com.example.common.events.IncidentFindRequest findRequest = com.example.common.events.IncidentFindRequest.newBuilder()
                .setId(request.getId())
                .setService(request.getService())
                .setPriority((request.getPriority() != null) ? request.getPriority() : null)
                .setStatus(request.getStatus() != null ? request.getStatus() : null)
                .build();

        kafkaTemplate.send(INCIDENT_FIND_REQUEST_TOPIC, uuid, findRequest)
                .whenComplete((result, ex) -> {
                    if(ex == null){
                        log.info("Событие отправлено");
                    }
                    else {
                        log.error("Ошибка при отправке");
                        future.completeExceptionally(ex);
                        pendingFindRequests.remove(uuid);
                    }
                });
        IncidentFindResponse response = future.get(30, TimeUnit.SECONDS);
        log.info("Событие получено");
        pendingFindRequests.remove(uuid);
        return response;
    }

    @KafkaListener(topics = INCIDENT_FIND_RESPONSE_TOPIC, groupId = "incident-producer-service-group")
    public void findIncidentResponse(com.example.common.events.IncidentFindResponse response){
        String uuid = String.valueOf(response.hashCode());
        CompletableFuture<com.example.common.events.IncidentFindResponse> future = new CompletableFuture<>();
        if(future != null){
            future.complete(response);
            pendingFindRequests.remove(uuid);
            log.info("Получен ответ поиска");
        }
    }

    @KafkaListener(topics = INCIDENT_RESPONSE_TOPIC, groupId = "incident-producer-service-group")
    public void createIncidentResponse(IncidentCreateResponse response) {
        String uuid = String.valueOf(response.getId());
        CompletableFuture<IncidentCreateResponse> future = pendingCreateRequests.get(uuid);
        if (future != null) {
            future.complete(response);
            pendingCreateRequests.remove(uuid);
            log.info("Получен ответ на создание инцидента");
        }
    }

}
