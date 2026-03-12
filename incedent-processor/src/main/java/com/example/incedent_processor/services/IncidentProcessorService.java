package com.example.incedent_processor.services;

import com.example.common.events.Alert;
import com.example.common.events.Incident;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class IncidentProcessorService {
    private  final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String INCIDENT_HIGH_PRIORITY_ALERT = "high-priority-alert";
    private static final String ALERT_TOPIC = "alert-topic";


    @KafkaListener(topics = INCIDENT_HIGH_PRIORITY_ALERT, groupId = "incident-processor-group")
    public void processIncident(Incident incident) {

        String responsibleGroup;

        switch (incident.getService()) {
            case "payment-service" :
                responsibleGroup = "payment-team";
            case "auth-service" :
                responsibleGroup = "auth-team";
            case "order-service"  :
                responsibleGroup = "order-team";
            case "inventory-service" :
                responsibleGroup = "inventory-team";
            default :
                responsibleGroup = "oncall-team";

        Alert alert = Alert.newBuilder()
                .setId(incident.getId())
                .setService(incident.getService())
                .setInfo(incident.getInfo())
                .setStatus(incident.getStatus())
                .setTimestamp(incident.getTimestamp())
                .setResponsibleGroup(responsibleGroup)
                .build();

        try{
            kafkaTemplate.send(ALERT_TOPIC, alert);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
}
