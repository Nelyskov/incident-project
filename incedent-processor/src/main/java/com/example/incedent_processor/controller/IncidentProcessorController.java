package com.example.incedent_processor.controller;

import com.example.common.events.*;
import com.example.incedent_processor.services.IncidentProcessorService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/incident-processor")
public class IncidentProcessorController {
    private final IncidentProcessorService incidentProcessorService;
    private final MeterRegistry meterRegistry;
    private final Counter totalRequestCounter;
    private final Counter totalErrorCounter;
    private final Timer proccessingTimer;


    public IncidentProcessorController(IncidentProcessorService incidentProcessorService,
                                       MeterRegistry meterRegistry){
        this.incidentProcessorService = incidentProcessorService;
        this.meterRegistry = meterRegistry;
        totalRequestCounter = Counter.builder("incident-processor.requests.total")
                .description("Общее количество запросов REST в incident-processor ")
                .tag("application", "incident-processor")
                .register(meterRegistry);
        totalErrorCounter = Counter.builder("incident-processor.errors.total")
                .description("Общее количество ошибок при выполнии запросов REST в incident-processor ")
                .tag("application", "incident-processor")
                .register(meterRegistry);
        proccessingTimer = Timer.builder("incident-processor.processing.timer")
                .description("Время обработки запросов")
                .tag("application", "incident-processor")
                .register(meterRegistry);
    }

    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        totalRequestCounter.increment();
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("Message", "incident-processor metrics");
        metrics.put("endpoints", Map.of(
                "prometheus", "/actuator/prometheus",
                "health", "/actuator/health",
                "metrics", "/actuator/metrics"
        ));

        metrics.put("kafka_metrics", List.of(
                "incidents.kafka.processing.time",
                "incidents.kafka.errors.total",
                "incidents.kafka.updated.priority.total",
                "incidents.kafka.updated.status.total",
                "incidents.kafka.created.total"
        ));

        metrics.put("api_metrics", List.of(
                "incident-processor.requests.total",
                "incident-processor.processing.timer",
                "incident-processor.error.total"
        ));
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String,String>> health(){
        Map<String, String> status = new HashMap<>();
        status.put("status", "UP");
        status.put("service", "incident-processor");
        return ResponseEntity.ok(status);
    }
}
