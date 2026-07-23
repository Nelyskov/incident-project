package com.example.incedent_processor;

import com.example.common.events.Alert;
import com.example.common.events.Incident;
import com.example.incedent_processor.services.IncidentProcessorService;
import com.example.incedent_processor.services.Service;
import io.micrometer.core.instrument.MeterRegistry;
import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Epic("Incident processor tests")
@ExtendWith(MockitoExtension.class)
public class IncidentProcessorServiceTest {

    private IncidentProcessorService service;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private MeterRegistry meterRegistry = new io.micrometer.core.instrument.simple.SimpleMeterRegistry();

    private Incident incident;

    @Captor
    ArgumentCaptor<String> alertTopic;

    @BeforeEach
    public void setup() {
        service = new IncidentProcessorService(kafkaTemplate, meterRegistry);
        incident = DataGenerationHelper.createIncidentWithHighPriority();
    }

    @ParameterizedTest
    @Story("Передаваемый сервис возвращает соответсующую команду")
    @CsvSource({
            "payment-service, payment-team",
            "auth-service, auth-team",
            "order-service, order-team",
            "inventory-service, inventory-team",
            "unknown-service, oncall-team"
    })
    public void incident_givenService_returnServiceTeam(
            String serviceName, String teamName
    ){
        incident.setService(serviceName);

        String serviceteam = Service.fromServiceName(incident.getService()).getResponsibleGroup();

        assertThat(serviceteam).isEqualTo(teamName);
    }

    @Test
    @Story("инцидент без сервиса вызывает oncall team")
    public void incident_nullService_returnOncallTeam() {
        incident.setService(null);

        String serviceTeam = Service.fromServiceName(null).getResponsibleGroup();

        assertThat(serviceTeam).isEqualTo("oncall-team");
    }

    @Test
    @Story("Incident с high priority отправляет в alert topic")
    public void highPriorityIncident_callProcessIncident_shouldSendKafkaMessage() {
        ConsumerRecord<String, Incident> record = new ConsumerRecord<>(
               "high-priority-alert",
               0,
               0L,
               "testUUID",
               incident
        );
        Acknowledgment ack = Mockito.mock(Acknowledgment.class);

        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);

        service.processIncident(record, ack);

        verify(kafkaTemplate).send(eq("alert-topic"), anyString(), any(Alert.class));
    }

    @Test
    @Story("при ошибке обработки инкрементируется счетчик")
    public void error_callKafkaProcessingErrors_shouldIncrement(){

        ConsumerRecord<String, Incident> record = new ConsumerRecord<>(
                "high-priority-alert",
                0,
                0L,
                "testUUID",
                incident
        );
        Acknowledgment ack = Mockito.mock(Acknowledgment.class);

        when(kafkaTemplate.send(anyString(), anyString(), any())).thenThrow(new RuntimeException("Kafka error"));
        assertThatThrownBy(() -> service.processIncident(record, ack))
                .isInstanceOf(RuntimeException.class);

        double errorCount = meterRegistry.counter("incidents.kafka.errors.total", "source", "kafka").count();
        assertThat(errorCount).isEqualTo(1.0);

    }
}