package com.example.alert_service;


import com.example.common.events.Alert;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class AlertConsumerService {
    private final EmailService emailService;
    private final Counter  alertsProcessed;
    private final Counter alertErrors;
    private static final Map<String, String> GROUP_EMAIL_MAP = Map.of(
            "payment-team",   "payment-team@company.com",
            "auth-team",      "auth-team@company.com",
            "order-team",     "order-team@company.com",
            "inventory-team", "inventory-team@company.com",
            "oncall-team",    "oncall-team@company.com"
    );

    public AlertConsumerService(EmailService emailService, MeterRegistry meterRegistry) {
        this.emailService = emailService;
        this.alertsProcessed = Counter.builder("alert.service.processed.total")
                .description("Количество обработанных алертов")
                .tag("application", "alert-service")
                .register(meterRegistry);
        this.alertErrors = Counter.builder("alert.service.errors.total")
                .description("Количество ошибок при обработке алертов")
                .tag("application", "alert-service")
                .register(meterRegistry);
    }

    @KafkaListener(topics = "alert-topic", groupId = "alert-service-group", containerFactory = "alertKafkaListenerContainerFactory")
    public void processAlert(ConsumerRecord<String, Alert> record, Acknowledgment ac) {
        Alert alert = record.value();
        log.info("Получен алерт. id: {}, service: {}, group: {}",
                alert.getId(), alert.getService(), alert.getResponsibleGroup());
        try {
            String recipientEmail = GROUP_EMAIL_MAP.getOrDefault(
                    alert.getResponsibleGroup().toString(),
                    "oncall-team@company.com"
            );

            String subject = String.format("[HIGH PRIORITY] Инцидент #%d в сервисе %s",
                    alert.getId(), alert.getService());

            String message = String.format(
                    "Инцидент требует немедленного внимания!\n\n" +
                            "ID: %d\n" +
                            "Сервис: %s\n" +
                            "Описание: %s\n" +
                            "Статус: %s\n" +
                            "Ответственная группа: %s\n" +
                            "Время: %d",
                    alert.getId(),
                    alert.getService(),
                    alert.getInfo(),
                    alert.getStatus(),
                    alert.getResponsibleGroup(),
                    alert.getTimestamp()
            );

            emailService.sendSimpleEmail(recipientEmail, subject, message);
            alertsProcessed.increment();
            log.info("Email отправлен группе {} на {}", alert.getResponsibleGroup(), recipientEmail);
            ac.acknowledge();

        }catch (Exception e){
            alertErrors.increment();
            log.error("Ошибка при обработке алерта id: {}: {}", alert.getId(), e.getMessage(), e);
            ac.acknowledge();
        }
    }
}
