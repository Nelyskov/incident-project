package com.example.ping_service;

import feign.Feign;
import feign.Logger;
import feign.slf4j.Slf4jLogger;
import lombok.Data;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "services")
public class ServiceConfig {

    private Service service;
    private Processor processor;
    private ProducerService producerService;

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

    @Bean
    public Feign.Builder feignBuilder() {
        return Feign.builder()
                .logger(new Slf4jLogger())
                .logLevel(Logger.Level.FULL)
                .requestInterceptor(template -> {
                    template.header("Content-Type", "application/json");
                    template.header("Accept", "application/json");
                });
    }

    @Bean
    @Qualifier("incidentServiceClient")
    public Service incidentServiceClient(Feign.Builder builder) {
        return builder.target(Service.class, service.getUrl());
    }

    @Bean
    @Qualifier("processorServiceClient")
    public Processor processorServiceClient(Feign.Builder builder) {
        return builder.target(Processor.class, processor.getUrl());
    }

    @Bean
    @Qualifier("producerServiceClient")
    public ProducerService producerServiceClient(Feign.Builder builder) {
        return builder.target(ProducerService.class, producerService.getUrl());
    }
}