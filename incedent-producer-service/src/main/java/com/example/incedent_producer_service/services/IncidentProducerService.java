package com.example.incedent_producer_service.services;

import com.example.common.events.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.util.concurrent.*;

@Service
@Slf4j
public class IncidentProducerService {

    private  final KafkaTemplate<String, Object> kafkaTemplate;

    public IncidentProducerService(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    private final ConcurrentHashMap<String, CompletableFuture<IncidentCreateResponse>> pendingCreateRequests = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CompletableFuture<IncidentFindResponse>> pendingFindRequests = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CompletableFuture<IncidentUpdateResponse>> pendingUpdateRequests = new ConcurrentHashMap<>();

    private static final String INCIDENT_CREATE_TOPIC        = "incident-create";
    private static final String INCIDENT_UPDATE_TOPIC        = "incident-update";
    private static final String INCIDENT_FIND_REQUEST_TOPIC  = "incident-find-request";
    private static final String INCIDENT_FIND_RESPONSE_TOPIC = "incident-find-response";
    private static final String INCIDENT_CREATE_RESPONSE_TOPIC = "incident-create-response";
    private static final String INCIDENT_UPDATE_RESPONSE_TOPIC = "incident-update-response";


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
                    if (ex == null) {
                        log.info("CREATE событие отправлено. uuid: {}", uuid);
                    } else {
                        log.error("Ошибка при отправке CREATE. uuid: {}", uuid, ex);
                        future.completeExceptionally(ex);
                        pendingCreateRequests.remove(uuid);
                    }
                });

        try {
            return future.get(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } finally {
            pendingCreateRequests.remove(uuid);
        }
    }

    public IncidentUpdateResponse updateIncident(IncidentUpdateRequest request)
            throws ExecutionException, InterruptedException, TimeoutException {
        String uuid = java.util.UUID.randomUUID().toString();

        CompletableFuture<IncidentUpdateResponse> future = new CompletableFuture<>();
        pendingUpdateRequests.put(uuid, future);
        log.info("Producer Service обработка запроса на обновление инцидента uuid {}", uuid);
        com.example.common.events.IncidentUpdateRequest updateRequest =  com.example.common.events.IncidentUpdateRequest.newBuilder()
                .setId(request.getId())
                .setService(request.getService())
                .setPriority(request.getPriority() != null ? request.getPriority() : null)
                .setStatus(request.getStatus() != null ? request.getStatus() : null)
                .build();
        kafkaTemplate.send(INCIDENT_UPDATE_TOPIC , uuid, updateRequest)
                .whenComplete((result, ex) -> {
                    if(ex == null)
                    {
                        log.info(" UPDATE Событие отправлено. uuid: {}", uuid);
                    }
                    else {
                        log.error("Ошибка при отправке UPDATE. uuid: {}", uuid, ex);
                        future.completeExceptionally(ex);
                        pendingCreateRequests.remove(uuid);
                    }
                });
        try {
            return future.get(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } finally {
            pendingUpdateRequests.remove(uuid);
        }
    }

    public IncidentFindResponse findIncidents(IncidentFindRequest request)
            throws ExecutionException, InterruptedException, TimeoutException {

        String uuid = java.util.UUID.randomUUID().toString();
        CompletableFuture<IncidentFindResponse> future = new CompletableFuture<>();
        log.info("Producer Service обработка запроса на поиск инцидента uuid {}", uuid);

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
                        log.info("Событие отправлено FIND uuid: {}", uuid);
                    }
                    else {
                        log.error("Ошибка при отправке FIND uuid: {}", uuid, ex);
                        future.completeExceptionally(ex);
                        pendingFindRequests.remove(uuid);
                    }
                });
        try {
            IncidentFindResponse response = future.get(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            log.info("FIND событие получено. uuid: {}", uuid);
            return response;
        } finally {
            pendingFindRequests.remove(uuid);
        }

    }

    @KafkaListener(
            topics = INCIDENT_CREATE_RESPONSE_TOPIC,
            groupId = "incident-producer-service-group",
            containerFactory = "incidentProducerServiceKafkaListener"
    )
    public void handleCreateResponse(
            ConsumerRecord<String, IncidentCreateResponse> record,
            Acknowledgment ack) {
        String uuid = record.key();
        CompletableFuture<IncidentCreateResponse> future = pendingCreateRequests.get(uuid);
        if (future != null) {
            future.complete(record.value());
            log.info("CREATE ответ получен. uuid: {}", uuid);
        } else {
            log.warn("Нет ожидающего CREATE future для uuid: {}", uuid);
        }
        ack.acknowledge();
    }

    @KafkaListener(
            topics = INCIDENT_UPDATE_RESPONSE_TOPIC,
            groupId = "incident-producer-service-group",
            containerFactory = "incidentProducerServiceKafkaListener"
    )
    public void handleUpdateResponse(
            ConsumerRecord<String, IncidentUpdateResponse> record,
            Acknowledgment ack) {
        String uuid = record.key();
        CompletableFuture<IncidentUpdateResponse> future = pendingUpdateRequests.get(uuid);
        if (future != null) {
            future.complete(record.value());
            log.info("UPDATE ответ получен. uuid: {}", uuid);
        } else {
            log.warn("Нет ожидающего UPDATE future для uuid: {}", uuid);
        }
        ack.acknowledge();
    }

    @KafkaListener(
            topics = INCIDENT_FIND_RESPONSE_TOPIC,
            groupId = "incident-producer-service-group",
            containerFactory = "incidentProducerServiceKafkaListener"
    )
    public void findIncidentResponse(
            ConsumerRecord<String, IncidentFindResponse> record,
            Acknowledgment ack) {
        String uuid = record.key();
        CompletableFuture<IncidentFindResponse> future = pendingFindRequests.get(uuid);
        if (future != null) {
            future.complete(record.value());
            log.info("FIND ответ получен. uuid: {}", uuid);
        } else {
            log.warn("Нет ожидающего FIND future для uuid: {}", uuid);
        }
        ack.acknowledge();
    }

}
