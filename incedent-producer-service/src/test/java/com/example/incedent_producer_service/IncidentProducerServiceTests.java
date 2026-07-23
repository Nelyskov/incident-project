package com.example.incedent_producer_service;

import com.example.common.events.IncidentCreateRequest;
import com.example.common.events.IncidentCreateResponse;
import com.example.common.events.IncidentPriority;
import com.example.incedent_producer_service.services.IncidentProducerService;
import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.SendResult;

import java.lang.reflect.Field;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import nl.altindag.log.LogCaptor;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

@Epic("Incident producer service test")
@ExtendWith(MockitoExtension.class)
public class IncidentProducerServiceTests {

    private IncidentProducerService service;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;


    @BeforeEach
    public void setup() {
        service = new IncidentProducerService(kafkaTemplate);

        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        future.complete(null);

        Mockito.lenient().doReturn(future)
                .when(kafkaTemplate)
                .send(Mockito.any(String.class), Mockito.any(String.class), Mockito.any());
    }

    @Test
    @Story("Создание запроса на создание инцидента вызывает топик incident-create")
    public void createRequest_getRequest_incidentResponseTopicShouldCallkafkaTemplate() throws Exception {
        IncidentCreateRequest request = new IncidentCreateRequest();
        request.setService("payment-service");
        request.setInfo("Test info");
        request.setPriority(IncidentPriority.HIGH);

        IncidentCreateResponse response = IncidentCreateResponse.newBuilder()
                .setId(1L)
                .setService("payment-service")
                .setInfo("Test info")
                .setStatus(com.example.common.events.IncidentStatus.CREATED)
                .setPriority(com.example.common.events.IncidentPriority.HIGH)
                .setTimestamp(System.currentTimeMillis())
                .build();

        CompletableFuture<IncidentCreateResponse> result = CompletableFuture.supplyAsync(() -> {
            try {
                return service.createIncident(request);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        Thread.sleep(100);

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> uuidCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> valueCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(
                topicCaptor.capture(),
                uuidCaptor.capture(),
                valueCaptor.capture()
        );
        assertThat(topicCaptor.getValue()).isEqualTo("incident-create");
        String uuid = uuidCaptor.getValue();

        ConsumerRecord<String, IncidentCreateResponse> record =
                new ConsumerRecord<>("incident-create-response", 0, 0L, uuid, response);
        Acknowledgment ack = Mockito.mock(Acknowledgment.class);
        service.handleCreateResponse(record, ack);

        IncidentCreateResponse actual = result.get(5, TimeUnit.SECONDS);
        assertThat(actual.getId()).isEqualTo(1L);
    }

    @Test
    @Story("Успешная обработка инцидента завершает future")
    public void handleCreateResponse_completeFuture() throws Exception {
        Field field = IncidentProducerService.class.getDeclaredField("pendingCreateRequests");
        field.setAccessible(true);

        ConcurrentHashMap<String, CompletableFuture<IncidentCreateResponse>> map =
                (ConcurrentHashMap<String, CompletableFuture<IncidentCreateResponse>>) field.get(service);

        CompletableFuture<IncidentCreateResponse> future = new CompletableFuture<>();
        map.put("test-uuid", future);

        ConsumerRecord<String, IncidentCreateResponse> record =
                new ConsumerRecord<>("incident-create-response", 0, 0L, "test-uuid", null);
        Acknowledgment ack = Mockito.mock(Acknowledgment.class);
        service.handleCreateResponse(record, ack);

        assertThat(future.isDone()).isTrue();
    }

    @Test
    @Story("Обработка по несуществующему UUID вызывает предупржедение в логах")
    public void handleCreateResponse_nonexistingUUID_logWarn() throws Exception{
        Field field = IncidentProducerService.class.getDeclaredField("pendingCreateRequests");
        field.setAccessible(true);

        LogCaptor captor = LogCaptor.forClass(IncidentProducerService.class);

        ConcurrentHashMap<String, CompletableFuture<IncidentCreateResponse>> map =
                (ConcurrentHashMap<String, CompletableFuture<IncidentCreateResponse>>) field.get(service);

        ConsumerRecord<String, IncidentCreateResponse> record =
                new ConsumerRecord<>("incident-create-response", 0, 0L, "test-uuid", null);
        Acknowledgment ack = Mockito.mock(Acknowledgment.class);
        service.handleCreateResponse(record, ack);

        assertThat(captor.getWarnLogs()).toString().contains("Нет ожидающего CREATE future для uuid");
    }
}
