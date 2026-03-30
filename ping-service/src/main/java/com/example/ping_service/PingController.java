package com.example.ping_service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Slf4j
public class PingController {
    private final IncidentServiceClient incidentServiceClient;
    private final ProcessorServiceClient processorServiceClient;
    private final ProducerServiceClient producerServiceClient;
    private final AlertServiceClient alertServiceClient;

    private final Counter totalRequestCounter;

    public PingController(IncidentServiceClient incidentServiceClient,
                          ProcessorServiceClient processorServiceClient,
                          ProducerServiceClient producerServiceClient,
                          MeterRegistry meterRegistry,
                          AlertServiceClient alertServiceClient) {
        this.incidentServiceClient = incidentServiceClient;
        this.processorServiceClient = processorServiceClient;
        this.producerServiceClient = producerServiceClient;
        this.alertServiceClient = alertServiceClient;

        totalRequestCounter = Counter.builder("ping-service.requests.total")
                .description("Общее количество запросов REST в ping service ")
                .tag("application", "ping-service")
                .register(meterRegistry);
    }

    @GetMapping("/ping")
    public Map<String, String> pingAllServices() {
        Map<String, String> map = new HashMap<>();
        log.info("GET /ping получение статуса всех сервисов");
        map.put("incident-service", checkService(incidentServiceClient :: pingService));
        map.put("incident-processor-service", checkService(processorServiceClient :: pingService));
        map.put("incident-producer-service", checkService(producerServiceClient :: pingService));
        map.put("alert-service", checkService(alertServiceClient::pingService));

        return map;
    }

    private String checkService(java.util.function.Supplier<String> supplier) {
        try{
            supplier.get();
            return "OK";
        }
        catch (Exception e){
            log.error("check service exception", e);
            return "down";
        }
    }

    ///  api для сбора метрик с сервиса

    @GetMapping("/stats")
    public ResponseEntity<Map<String, String>> stats() {
        Map<String, String> stats = new HashMap<>();
        stats.put("message", "Используйте /actuator/prometheus для просмотра всех метрик");
        stats.put("prometheus_endpoint", "/actuator/prometheus");
        stats.put("metrics_endpoint", "/actuator/metrics");
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        totalRequestCounter.increment();
        log.info("GET /metrics получение метрик всех сервисов");
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("message", "ping-service metrics");
        metrics.put("endpoints", Map.of(
                "prometheus", "/actuator/prometheus",
                "health", "/actuator/health",
                "metrics", "/actuator/metrics"
        ));
        metrics.put("kafka_metrics", List.of(
                "incidents.kafka.processing.time",
                "incidents.kafka.errors.total",
                "incidents.kafka.created.total"
        ));
        metrics.put("api_metrics", List.of(
                "ping-service.requests.total",
                "ping-service.processing.timer",
                "ping-service.errors.total"
        ));
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "UP");
        status.put("service", "ping-service");
        return ResponseEntity.ok(status);
    }
}

