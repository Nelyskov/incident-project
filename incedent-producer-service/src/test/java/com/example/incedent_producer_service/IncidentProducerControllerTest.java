package com.example.incedent_producer_service;

import com.example.common.events.IncidentCreateResponse;
import com.example.common.events.IncidentFindResponse;
import com.example.common.events.IncidentStatus;
import com.example.common.events.IncidentUpdateResponse;
import com.example.incedent_producer_service.controller.IncidentProducerServiceController;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.incedent_producer_service.services.IncidentProducerService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;


import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@Epic("Incident producer service Controller tests")
@WebMvcTest(IncidentProducerServiceController.class)
public class IncidentProducerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IncidentProducerService service;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }

    @Autowired
    private ObjectMapper mapper;

    @Test
    @Story("Создание инцидента возвращает 201")
    public void postCreate_shouldReturn201() throws Exception {
        IncidentCreateResponse response = IncidentCreateResponse.newBuilder()
                .setId(1L)
                .setService("payment-service")
                .setInfo("Test info")
                .setStatus(com.example.common.events.IncidentStatus.CREATED)
                .setPriority(com.example.common.events.IncidentPriority.HIGH)
                .setTimestamp(System.currentTimeMillis())
                .build();

        when(service.createIncident(any())).thenReturn(response);
        String requestBody = """
                {
                    "service": "payment-service",
                    "info": "Test info",
                    "priority": "HIGH"
                }
                """;

        mockMvc.perform(post("/api/incident-producer-service/create")
                        .contentType(APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated()).andExpect(status().is(201));
    }

    @Test
    @Story("Таймаут ответ возвращает 500")
    public void postCreate_timeOut_shouldRetur500() throws Exception {
        when(service.createIncident(any()))
                .thenThrow(new TimeoutException("Сервис не ответил вовремя"));

        String requestBody = """
                {
                    "service": "payment-service",
                    "info": "Test info",
                    "priority": "HIGH"
                }
                """;

        mockMvc.perform(post("/api/incident-producer-service/create")
                .contentType(APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().is(500));
    }

    @Test
    @Story("Успешное обновление возвращает 200")
    public void putUpdate_success_shouldReturn200() throws Exception {
        IncidentUpdateResponse response = IncidentUpdateResponse.newBuilder()
                .setId(1L)
                .setInfo("test info")
                .setPriority(com.example.common.events.IncidentPriority.HIGH)
                .setService("payment-service")
                .setStatus(IncidentStatus.PROCESSING)
                .setUpdatedAt(System.currentTimeMillis())
                .setTimestamp(System.currentTimeMillis())
                .build();

        when(service.updateIncident(any())).thenReturn(response);

        String requestBody = """
                {
                    "id" : "1",
                    "status" : "PROCESSING"
                }
                """;

        mockMvc.perform(put("/api/incident-producer-service/update")
                .contentType(APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk()).andExpect(status().is(200));

    }

    @Test
    @Story("Получение существующего инцидента по id возвращает 200")
    public void get_existingIncident_shouldReturn200() throws Exception {
        com.example.common.events.Incident incident = com.example.common.events.Incident.newBuilder()
                .setId(1L)
                .setService("payment-service")
                .setInfo("Test info")
                .setStatus(com.example.common.events.IncidentStatus.CREATED)
                .setPriority(com.example.common.events.IncidentPriority.HIGH)
                .setTimestamp(System.currentTimeMillis())
                .build();

        IncidentFindResponse response = IncidentFindResponse.newBuilder().setIncidents(List.of(incident)).build();

        when(service.findIncidents(any())).thenReturn(response);

        String requestBody = """
                {
                    "id" : "1"
               }
        """;

        mockMvc.perform(get("/api/incident-producer-service/"+incident.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(incident.getId()));
    }
}
