package com.example.ping_service;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceHealth {
    private boolean healthy;
    private String response;
    private String status;
    private Map<String, Object> metrics;
    private Map<String, Object> additionalInfo;
    private String errorMessage;
    private long responseTime;
    private LocalDateTime timestamp;
}
