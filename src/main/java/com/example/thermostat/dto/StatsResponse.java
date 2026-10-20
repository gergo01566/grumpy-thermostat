package com.example.thermostat.dto;

import com.example.thermostat.model.Mode;
import com.example.thermostat.model.Statistic;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record StatsResponse(
        Mode mode,
        @JsonInclude(JsonInclude.Include.NON_NULL) Instant from,
        @JsonInclude(JsonInclude.Include.NON_NULL) Instant to,
        @JsonInclude(JsonInclude.Include.NON_NULL) Statistic statistic,
        Map<String, String> units,
        List<DeviceStats> devices
) {}
