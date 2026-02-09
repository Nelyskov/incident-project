package com.example.incedent_service.controller;

import com.example.incedent_service.entities.CreateIncidentRequest;
import com.example.incedent_service.entities.Incident;
import com.example.incedent_service.entities.IncidentResponse;
import com.example.incedent_service.entities.UpdateIncidentStatusRequest;
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
//    private final IncidentService incidentService;
    private final Counter requestCounter;
//    private final Counter incidentCreatedCounter;
//    private final Counter incidentUpdatedCounter;
    private final Counter incidentErrorCounter;
    private final Timer incidentProcessigTimer;

    public IncidentServiceController( MeterRegistry meterRegistry){
//        incidentService = service;
        this.requestCounter = Counter.builder("incident-service.requests.total")
                .description("Общее количество запросов в IncidentService")
                .tag("application", "incident-service")
                .register(meterRegistry);

//        incidentCreatedCounter = Counter.builder("incident-service.incidents.created.total")
//                .description("Общее количество запросов созданных инцидентов x")
//                .tag("application", "incident-service")
//                .register(meterRegistry);
//        incidentUpdatedCounter = Counter.builder("incident-service.incidents.updated.total")
//                .description("Общее количество запросов обновленных инцидентов")
//                .tag("application", "incident-service")
//                .register(meterRegistry);
        incidentProcessigTimer = Timer.builder("incident-service.processing.timer")
                .description("Время обработки запросов")
                .tag("application", "incident-service")
                .register(meterRegistry);
        incidentErrorCounter = Counter.builder("incident-service.incidents.error.total")
                .description("Общее количество ошибок")
                .tag("application", "incident-service")
                .register(meterRegistry);
    }

//    @PostMapping()
//    public ResponseEntity<IncidentResponse> createIncident(@RequestBody CreateIncidentRequest request) throws Exception{
//        return incidentProcessigTimer.recordCallable(() -> {
//            try{
//                requestCounter.increment();
//                IncidentResponse response = incidentService.createIncident(request);
//                incidentCreatedCounter.increment();
//                return ResponseEntity.ok(response);
//            } catch (Exception e) {
//                incidentErrorCounter.increment();
//                throw new RuntimeException(e);
//            }
//        });
//    }

//    @GetMapping("/{id{")
//    public ResponseEntity<IncidentResponse> getIncident(@PathVariable Long id) throws Exception {
//        return incidentProcessigTimer.recordCallable(() -> {
//            try{
//                requestCounter.increment();
//                IncidentResponse response = incidentService.getIncidentById(id);
//                return ResponseEntity.ok(response);
//            }catch (Exception e){
//                incidentErrorCounter.increment();
//                throw new RuntimeException(e);
//            }
//        });
//    }

//    @GetMapping
//    public ResponseEntity<List<IncidentResponse>> getAllIncidents() throws Exception {
//        return incidentProcessigTimer.recordCallable(() -> {
//            try{
//                requestCounter.increment();
//                return ResponseEntity.ok(incidentService.getAllIncidents());
//            }catch (Exception e){
//                incidentErrorCounter.increment();
//                throw new RuntimeException();
//            }
//        });
//    }

//    @PatchMapping("/{id}/status")
//    public ResponseEntity<IncidentResponse> updateIncidentStatus(@PathVariable Long id, @RequestBody UpdateIncidentStatusRequest request) throws Exception {
//        return incidentProcessigTimer.recordCallable(() -> {
//            try{
//                requestCounter.increment();
//                IncidentResponse response = incidentService.updateIncident(id, request);
//                incidentUpdatedCounter.increment();
//                return ResponseEntity.ok(response);
//            }catch (Exception e){
//                incidentErrorCounter.increment();
//                throw new RuntimeException(e);
//            }
//        });
//    }

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
                "incidents.kafka.updated.priority.total",
                "incidents.kafka.updated.status.total",
                "incidents.kafka.created.total"
        ));

        metrics.put("api_metrics", List.of(
                "incident-service.requests.total",
                "incident-service.processing.timer",
                "incident-service.incidents.error.total"
        ));
        return ResponseEntity.ok(metrics);
    }


}
