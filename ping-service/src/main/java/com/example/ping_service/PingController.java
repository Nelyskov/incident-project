package com.example.ping_service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@Slf4j
public class PingController {

    @Value("${services.service.url}")
    private String incidentUrl;

    @Value("${services.processor.url}")
    private String processorUrl;

    @Value("${services.producerService.url}")
    private String producerUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/ping")
    public Map<String, String> pingAllServices() {

        Map<String, String> result = new HashMap<>();

        result.put("incident-service", checkService(incidentUrl + "/health"));
        result.put("processor-service", checkService(processorUrl + "/actuator/health"));
        result.put("producer-service", checkService(producerUrl + "/health"));

        return result;
    }

    private String checkService(String url) {
        try {
            String response = restTemplate.getForObject(url, String.class);
            return "UP";
        } catch (Exception e) {
            return "DOWN";
        }
    }
}

