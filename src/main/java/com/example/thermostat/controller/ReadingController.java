package com.example.thermostat.controller;

import com.example.thermostat.dto.StatsResponse;
import com.example.thermostat.model.Metric;
import com.example.thermostat.model.Statistic;
import com.example.thermostat.service.ReadingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/readings")
public class ReadingController {

    private final ReadingService service;

    public ReadingController(ReadingService service) {
        this.service = service;
    }

    @GetMapping("/stats")
    public StatsResponse stats(
            @RequestParam(required = false) Set<String> deviceIds,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) Set<String> metrics,
            @RequestParam(required = false) String statistic
    ) {
        Set<Metric> resolvedMetrics = parseMetrics(metrics);
        Statistic resolvedStatistic = parseStatistic(statistic);

        return service.query(deviceIds, from, to, resolvedMetrics, resolvedStatistic);
    }

    private Set<Metric> parseMetrics(Set<String> metrics) {
        if (metrics == null || metrics.isEmpty()) return null;
        return metrics.stream()
                .map(Metric::parse)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(Metric.class)));
    }

    private Statistic parseStatistic(String statistic) {
        if (statistic == null) return null;
        return Statistic.parse(statistic);
    }
}