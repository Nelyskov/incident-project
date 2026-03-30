package com.example.incedent_processor.services;

import com.example.common.events.Alert;
import com.example.common.events.Incident;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class IncidentProcessorService {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final MeterRegistry meterRegistry;

    private final Counter kafkaIncidentsProcessed;
    private final Counter kafkaProcessingErrors;
    private final Timer kafkaProcessingTimer;

    private static final String INCIDENT_HIGH_PRIORITY_ALERT = "high-priority-alert";
    private static final String ALERT_TOPIC = "alert-topic";

    public IncidentProcessorService(
            KafkaTemplate<String, Object> kafkaTemplate,
            MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.meterRegistry = meterRegistry;

        kafkaIncidentsProcessed = Counter.builder("incidents.kafka.created.total")
                .description("Обработано HIGH PRIORITY инцидентов")
                .tag("source", "kafka")
                .register(meterRegistry);
        kafkaProcessingErrors = Counter.builder("incidents.kafka.errors.total")
                .description("Ошибки обработки")
                .tag("source", "kafka")
                .register(meterRegistry);
        kafkaProcessingTimer = Timer.builder("incidents.kafka.processing.time")
                .description("Время обработки инцидентов")
                .tag("source", "kafka")
                .register(meterRegistry);
    }

    @KafkaListener(topics = INCIDENT_HIGH_PRIORITY_ALERT,
            groupId = "incident-processor-group",
            containerFactory = "incidentProducerServiceKafkaListener")
    public void processIncident(ConsumerRecord<String, Incident> record,
                                Acknowledgment ack) {

        Timer.Sample timer = Timer.start(meterRegistry);
        String uuid = record.key();
        Incident incident = record.value();
        String responsibleGroup;

        log.debug("Получен HIGH PRIORITY инцидент. uuid: {}, offset: {}", uuid, record.offset());
        log.info("Обработка HIGH PRIORITY инцидента. id: {}, uuid: {}, service: {}",
                incident.getId(), uuid, incident.getService());

        switch (incident.getService()) {
            case "payment-service":
                responsibleGroup = "payment-team";
                break;
            case "auth-service":
                responsibleGroup = "auth-team";
                break;
            case "order-service":
                responsibleGroup = "order-team";
                break;
            case "inventory-service":
                responsibleGroup = "inventory-team";
                break;
            default:
                responsibleGroup = "oncall-team";
                break;
        }

            Alert alert = Alert.newBuilder()
                    .setId(incident.getId())
                    .setService(incident.getService())
                    .setInfo(incident.getInfo())
                    .setStatus(incident.getStatus())
                    .setTimestamp(incident.getTimestamp())
                    .setResponsibleGroup(responsibleGroup)
                    .build();

        try{
            kafkaTemplate.send(ALERT_TOPIC, uuid, alert)
                    .whenComplete((res, e) -> {
                        if(e == null)
                            log.info("Alert отправлен{}, group: {}, uuid {}",incident.getId(), responsibleGroup, uuid);
                        else
                            log.info("Ошибка при отправлке Alert id{}, uuid {}", incident.getId(), uuid, e);
                    });
            kafkaIncidentsProcessed.increment();
            ack.acknowledge();
        } catch (Exception e) {
            kafkaProcessingErrors.increment();
            log.error("Ошибка при обработке инцидента id: {}: {}", incident.getId(), e.getMessage(), e);
            ack.acknowledge();
            throw new RuntimeException("Ошибка при обработке инцидента", e);
        } finally {
            timer.stop(kafkaProcessingTimer);
        }
    }
}

