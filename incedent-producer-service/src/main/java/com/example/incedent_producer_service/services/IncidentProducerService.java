package com.example.incedent_producer_service.services;

import com.example.incedent_producer_service.config.KafkaConfig;
import com.example.incedent_producer_service.entities.CreateIncidentRequest;
import com.example.incedent_producer_service.entities.Incident;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class IncidentProducerService {

    private  final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String INCIDENT_CREATE_TOPIC = "incident-create";
    private static final String INCIDENT_UPDATE_STATUS_TOPIC = "incident-status-update";
    private static final String INCIDENT_UPDATE_PRIORITY_TOPIC = "incident-priority-update";
    private static final String INCIDENT_FIND_TOPIC = "incident-find";
    private static final String INCIDENT_HIGH_PRIORITY_ALERT = "high-priority-alert";

    public Incident createIncident(CreateIncidentRequest request){
        kafkaTemplate.send(INCIDENT_CREATE_TOPIC, request)
                .whenComplete((result, ex) -> {
                    if(ex == null){
                        log.info("CreateIncidentRequest отправлен в incident-service");
                    }else
                    {
                        log.info("Ошибка при отправке в incident service");
                    }
                });
    }


}
