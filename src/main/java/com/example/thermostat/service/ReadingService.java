package com.example.thermostat.service;

import com.example.thermostat.dto.DeviceStats;
import com.example.thermostat.dto.StatsResponse;
import com.example.thermostat.exception.DeviceNotFoundException;
import com.example.thermostat.exception.InvalidQueryException;
import com.example.thermostat.model.Metric;
import com.example.thermostat.model.Mode;
import com.example.thermostat.model.Statistic;
import com.example.thermostat.model.ThermostatReading;
import com.example.thermostat.repository.ThermostatReadingRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReadingService {

    private final ThermostatReadingRepository repository;

    public ReadingService(ThermostatReadingRepository repository) {
        this.repository = repository;
    }

    public StatsResponse query(Set<String> deviceIds, Instant from, Instant to,
                               Set<Metric> metrics, Statistic statistic) {

        // 1. Paraméterek feloldása
        Set<String> normalized = deviceIds == null ? Set.of() : deviceIds.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(TreeSet::new));
        Set<String> resolvedDevices = normalized.isEmpty()
                ? new TreeSet<>(repository.deviceIds())
                : normalized;
        Set<Metric> resolvedMetrics = metrics == null || metrics.isEmpty()
                ? EnumSet.allOf(Metric.class)
                : EnumSet.copyOf(metrics);
        Statistic resolvedStatistic = statistic != null ? statistic : Statistic.AVG;

        // 2. Dátum validáció
        boolean latestMode = (from == null && to == null);
        if (!latestMode) validateDates(from, to);

        // 3. Eszköz validáció
        requireKnownDevices(resolvedDevices);

        // 4. Aggregáció és válasz összeállítása
        List<DeviceStats> deviceStatsList = new ArrayList<>();
        for (String deviceId : resolvedDevices) {
            List<ThermostatReading> readings = latestMode
                    ? repository.findAll(deviceId)
                    : repository.findInRange(deviceId, from, to);

            Map<String, Double> metricValues = new LinkedHashMap<>();
            for (Metric metric : resolvedMetrics) {
                Double raw = latestMode
                        ? lastNonNull(readings, metric)
                        : aggregate(readings, metric, resolvedStatistic);
                metricValues.put(metric.name().toLowerCase(), round(raw));
            }
            deviceStatsList.add(new DeviceStats(deviceId, metricValues));
        }

        Mode mode = latestMode ? Mode.LATEST : Mode.RANGE;
        return new StatsResponse(mode, from, to, latestMode ? null : resolvedStatistic,
                Map.of("temperature", "C", "humidity", "%"), deviceStatsList);
    }

    private void validateDates(Instant from, Instant to) {
        if (from == null || to == null) {
            throw new InvalidQueryException("Both 'from' and 'to' must be provided together");
        }
        if (!from.isBefore(to)) {
            throw new InvalidQueryException("'from' must be before 'to'");
        }
    }

    private void requireKnownDevices(Set<String> deviceIds) {
        Set<String> unknown = deviceIds.stream()
                .filter(id -> !repository.exists(id))
                .collect(Collectors.toCollection(TreeSet::new));
        if (!unknown.isEmpty()) {
            throw new DeviceNotFoundException(unknown);
        }
    }

    private Double lastNonNull(List<ThermostatReading> readings, Metric metric) {
        for (int i = readings.size() - 1; i >= 0; i--) {
            Double value = metric.getter().apply(readings.get(i));
            if (value != null) return value;
        }
        return null;
    }

    private Double aggregate(List<ThermostatReading> readings, Metric metric, Statistic statistic) {
        OptionalDouble result = statistic.apply(
                readings.stream()
                        .map(metric.getter())
                        .filter(Objects::nonNull)
                        .mapToDouble(Double::doubleValue)
        );
        return result.isPresent() ? result.getAsDouble() : null;
    }

    private Double round(Double value) {
        if (value == null) return null;
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}