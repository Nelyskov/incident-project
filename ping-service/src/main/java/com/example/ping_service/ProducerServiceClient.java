package com.example.ping_service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@FeignClient(name = "producer-service", url = "${services.producerService.url}")
public interface ProducerServiceClient {

    @GetMapping("${services.producerService.healthEndpoint}")
    String pingService();

    @GetMapping("/api/status")
    String getServiceStatus();

    @GetMapping("/api/metrics")
    Map<String, Object> getMetrics();
}
