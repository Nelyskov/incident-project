package com.example.incedent_producer_service.controller;


import com.example.incedent_producer_service.entities.CreateIncidentRequest;
import com.example.incedent_producer_service.services.IncidentProducerService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/incident-producer-service")
public class IncidentProducerServiceController {
    private final IncidentProducerService service;
    private final Counter totalRequestCounter;
    private final Counter totalErrorCounter;
    private final Timer proccessingTimer;

    public IncidentProducerServiceController(IncidentProducerService service, MeterRegistry registry){
        this.service = service;

        totalRequestCounter = Counter.builder("incident-producer-service.requests.total")
                .description("Общее количество запросов REST в incident producer service ")
                .tag("application", "incident-producer-service")
                .register(registry);

        totalErrorCounter = Counter.builder("incident-producer-service.errors.total")
                .description("Общее количество ошибок при выполнии запросов REST в incident producer service ")
                .tag("application", "incident-producer-service")
                .register(registry);
        proccessingTimer = Timer.builder("incident-producer-service.processing.timer")
                .description("Время обработки запросов")
                .tag("application", "incident-producer-service")
                .register(registry);
    }

    @PostMapping()
    public ResponseEntity<Object> createIncident(@RequestBody CreateIncidentRequest request) throws Exception{

    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> getIncidentById(@PathVariable long id) throws Exception{

    }

    @GetMapping()
    public ResponseEntity<Object> getAllIncidents() throws Exception{

    }


    ///  api для сбора метрик с сервиса

    @GetMapping("/stats")
    public ResponseEntity<Map<String, String>> stats(){
        Map<String, String> stats = new HashMap<>();
        stats.put("message", "API /actuator/prometheus для просмотра всех метрик");
        stats.put("prometheus_endpoint", "http://localhost:8080/actuator/prometheus");
        stats.put("metrics_endpoint", "http://localhost:8080/actuator/metrics");
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        totalRequestCounter.increment();
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("Message", "incident producer service metrics");
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
                "incident-producer-service.requests.total",
                "incident-producer-service.processing.timer",
                "incident-producer-service.error.total"
        ));
        return ResponseEntity.ok(metrics);
    }
}
