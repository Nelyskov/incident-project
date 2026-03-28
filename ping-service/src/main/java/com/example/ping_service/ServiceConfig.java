package com.example.ping_service;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "services")
public class ServiceConfig {

    private Service service;
    private Processor processor;
    private ProducerService producerService;
    private AlertService alertService;

    @Data
    public static class Service {
        private String url;
        private String healthEndpoint;
    }

    @Data
    public static class Processor {
        private String url;
        private String healthEndpoint;
    }

    @Data
    public static class ProducerService {
        private String url;
        private String healthEndpoint;
    }

    @Data
    public static class AlertService {
        private String url;
        private String healthEndpoint;
    }
}