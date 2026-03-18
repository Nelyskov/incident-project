package com.example.ping_service;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceDetails {
    private String name;
    private String url;
    private String healthEndpoint;
    private int timeout;
    private int retryCount;
    private boolean status;
}
