package com.example.ping_service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@FeignClient(name = "processor-service", url = "${services.processor.url}")
public interface ProcessorServiceClient {

    @GetMapping("/health")
    String pingService();

    @GetMapping("/metrics")
    Map<String, Object> getMetrics();
}
