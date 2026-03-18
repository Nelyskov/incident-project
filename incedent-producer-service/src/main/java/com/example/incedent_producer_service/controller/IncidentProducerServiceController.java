package com.example.incedent_producer_service.controller;


import com.example.common.events.*;
import com.example.incedent_producer_service.services.IncidentProducerService;
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
@RequestMapping("/api/incident-producer-service")
public class IncidentProducerServiceController {
    private final IncidentProducerService service;
    private final MeterRegistry meterRegistry;
    private final Counter totalRequestCounter;
    private final Counter totalErrorCounter;
    private final Timer proccessingTimer;

    public IncidentProducerServiceController(IncidentProducerService service, MeterRegistry meterRegistry) {
        this.service = service;
        this.meterRegistry = meterRegistry;

        totalRequestCounter = Counter.builder("incident-producer-service.requests.total")
                .description("Общее количество запросов REST в incident producer service ")
                .tag("application", "incident-producer-service")
                .register(meterRegistry);

        totalErrorCounter = Counter.builder("incident-producer-service.errors.total")
                .description("Общее количество ошибок REST")
                .tag("application", "incident-producer-service")
                .register(meterRegistry);

        proccessingTimer = Timer.builder("incident-producer-service.processing.timer")
                .description("Время обработки запросов")
                .tag("application", "incident-producer-service")
                .register(meterRegistry);
    }

    @PostMapping("/create")
    public ResponseEntity<Object> createIncident(@RequestBody IncidentCreateRequest request) throws Exception {
        totalRequestCounter.increment();
        return proccessingTimer.record(() -> {
            try {
                IncidentCreateResponse createdEvent = service.createIncident(request);

                Map<String, Object> response = new HashMap<>();
                response.put("message", "Инцидент успешно создан");
                response.put("incidentId", createdEvent.getId());
                response.put("service", createdEvent.getService());
                response.put("info", createdEvent.getInfo());
                response.put("priority", createdEvent.getPriority());
                response.put("status", createdEvent.getStatus());
                response.put("time", createdEvent.getTimestamp());
                return ResponseEntity.status(HttpStatus.CREATED).body(response);

            }  catch (Exception e) {
                totalErrorCounter.increment();
                return ResponseEntity.internalServerError().body(e.getMessage());
            }
        });
    }

    @PutMapping("/update")
    public ResponseEntity<Object> updateIncident(@RequestBody IncidentUpdateRequest request) throws Exception{
        totalRequestCounter.increment();
        return proccessingTimer.recordCallable(() -> {
            try{
                IncidentUpdateResponse updateResponse=  service.updateIncident(request);

                Map<String, Object> response = new HashMap<>();
                response.put("message", "Инцидент успешно обновлен");
                response.put("incidentId", updateResponse.getId());
                response.put("service", updateResponse.getService());
                response.put("info", updateResponse.getInfo());
                response.put("status", updateResponse.getStatus());
                response.put("priority", updateResponse.getPriority());
                response.put("updatedAt", updateResponse.getUpdatedAt());
                return ResponseEntity.ok(response);

            } catch (Exception e) {
                totalErrorCounter.increment();
                return ResponseEntity.internalServerError().body(e.getMessage());
            }
        });
    }

    @GetMapping("/{ID}")
    public ResponseEntity<Object> getIncidentById(@PathVariable Long ID) throws Exception{
        totalRequestCounter.increment();
        IncidentFindRequest request = IncidentFindRequest.newBuilder().setId(ID).build();

        try {
            IncidentFindResponse incident = service.findIncidents(request);

            if (!incident.getIncidents().isEmpty()) {
                return ResponseEntity.ok(incident);
            } else {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Инцидент не найден");
                errorResponse.put("message", "Инцидент не найден " + ID );
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }
        } catch (Exception e) {
            totalErrorCounter.increment();
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<Object> getAllIncidents(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String service,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String status) {
        totalRequestCounter.increment();
        IncidentFindRequest.Builder builder = IncidentFindRequest.newBuilder()
                .setId(id)
                .setService(service);

            if (priority != null) {
                builder.setPriority(String.valueOf(IncidentPriority.valueOf(priority)));
            }
            if (status != null) {
                builder.setStatus(String.valueOf(IncidentStatus.valueOf(status)));
            }

        try {
            IncidentFindResponse response = this.service.findIncidents(builder.build());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            totalErrorCounter.increment();
            return ResponseEntity.internalServerError().body(e.getMessage());
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
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("message", "incident-producer-service metrics");
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
                "incident-producer-service.requests.total",
                "incident-producer-service.processing.timer",
                "incident-producer-service.errors.total"
        ));
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "UP");
        status.put("service", "incident-producer-service");
        return ResponseEntity.ok(status);
    }
}


