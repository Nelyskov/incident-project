package com.example.ping_service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@FeignClient(name = "incident-service", url = "${services.service.url}")
public interface IncidentServiceClient {

    @GetMapping("/health")
    String pingService();

    @GetMapping("/api/status")
    String getServiceStatus();

    @GetMapping("/api/metrics")
    Map<String, Object> getMetrics();
}