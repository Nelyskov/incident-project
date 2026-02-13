package com.example.incedent_service.controller;


import com.example.incedent_service.services.IncidentService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/incident-service")
public class IncidentServiceController {
    private final IncidentService incidentService;
    private final MeterRegistry meterRegistry;

    private final Counter requestCounter;
    private final Counter incidentErrorCounter;
    private final Timer incidentProcessigTimer;

    public IncidentServiceController(IncidentService incidentService, MeterRegistry meterRegistry1, MeterRegistry meterRegistry){
        this.incidentService = incidentService;
        this.meterRegistry = meterRegistry1;
        this.requestCounter = Counter.builder("incident-service.requests.total")
                .description("Общее количество REST запросов")
                .tag("application", "incident-service")
                .register(meterRegistry);


        incidentProcessigTimer = Timer.builder("incident-service.processing.timer")
                .description("Время обработки REST запросов")
                .tag("application", "incident-service")
                .register(meterRegistry);
        incidentErrorCounter = Counter.builder("incident-service.incidents.error.total")
                .description("Общее количество ошибок в REST запросах")
                .tag("application", "incident-service")
                .register(meterRegistry);
    }

    @GetMapping("/healt")
    public ResponseEntity<Map<String,String>> health(){
        Map<String, String> status = new HashMap<>();
        status.put("status", "UP");
        status.put("service", "incident-service");
        return ResponseEntity.ok(status);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, String>> stats(){
        Map<String, String> stats = new HashMap<>();
        stats.put("message", "API /actuator/prometheus для просмотра всех метрик");
        stats.put("prometheus_endpoint", "http://localhost:8080/actuator/prometheus");
        stats.put("metrics_endpoint", "http://localhost:8080/actuator/metrics");
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics(){
        requestCounter.increment();
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("Message", "Incident service metrics");
        metrics.put("endpoints", Map.of(
                "prometheus", "/actuator/prometheus",
                "health", "/actuator/health",
                "metrics", "/actuator/metrics"
        ));

        metrics.put("kafka_metrics", List.of(
                "incidents.kafka.processing.time",
                "incidents.kafka.errors.total",
                "incidents.kafka.updated.total",
                "incidents.kafka.created.total",
                "incidents.kafka.found.total"
        ));

        metrics.put("api_metrics", List.of(
                "incident-service.rest.requests.total",
                "incident-service.rest.processing.timer",
                "incident-service.rest.incidents.error.total"
        ));
        return ResponseEntity.ok(metrics);
    }


}
