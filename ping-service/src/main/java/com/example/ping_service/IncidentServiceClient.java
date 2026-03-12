package com.example.ping_service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "incident-service", url = "${services.service.url}")
public interface IncidentServiceClient {

    @GetMapping("${services.service.healthEndpoint}")
    String pingService();

    @GetMapping("/api/status")
    String getServiceStatus();

    @GetMapping("/api/metrics")
    Map<String, Object> getMetrics();
}
