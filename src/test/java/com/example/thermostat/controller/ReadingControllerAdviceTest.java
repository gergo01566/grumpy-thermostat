package com.example.thermostat.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class ReadingControllerAdviceTest {

    @Autowired
    WebApplicationContext context;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    private static final String BASE = "/api/v1/readings/stats";

    @Test
    void unknown_metric_returns_400() throws Exception {
        mockMvc.perform(get(BASE).param("metrics", "pressure"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Unknown metric 'pressure'. Allowed values: temperature, humidity"));
    }

    @Test
    void invalid_date_format_returns_400() throws Exception {
        mockMvc.perform(get(BASE).param("from", "abc").param("to", "2026-07-12T00:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Invalid value for parameter 'from': abc"));
    }

    @Test
    void unknown_device_returns_404_with_ids() throws Exception {
        mockMvc.perform(get(BASE).param("deviceIds", "1,99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Unknown device(s): 99"))
                .andExpect(jsonPath("$.unknownIds[0]").value("99"));
    }

    @Test
    void unknown_path_returns_404_not_500() throws Exception {
        mockMvc.perform(get("/api/v1/foo"))
                .andExpect(status().isNotFound());
    }

    @Test
    void wrong_http_method_returns_405_not_500() throws Exception {
        mockMvc.perform(post(BASE))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void get_stats_returns_200_with_expected_structure() throws Exception {
        mockMvc.perform(get(BASE)
                        .param("deviceIds", "1")
                        .param("metrics", "temperature")
                        .param("statistic", "avg")
                        .param("from", "2026-07-10T00:00:00Z")
                        .param("to", "2026-07-12T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("RANGE"))
                .andExpect(jsonPath("$.statistic").value("AVG"))
                .andExpect(jsonPath("$.units.temperature").value("C"))
                .andExpect(jsonPath("$.units.humidity").value("%"))
                .andExpect(jsonPath("$.from").value("2026-07-10T00:00:00Z"))
                .andExpect(jsonPath("$.to").value("2026-07-12T00:00:00Z"))
                .andExpect(jsonPath("$.devices[0].deviceId").value("1"))
                .andExpect(jsonPath("$.devices[0].metrics.temperature").value(24.25));
    }
}