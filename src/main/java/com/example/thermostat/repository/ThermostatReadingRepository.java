package com.example.thermostat.repository;

import com.example.thermostat.config.DataProperties;
import com.example.thermostat.csv.CsvLoader;
import com.example.thermostat.model.ThermostatReading;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Repository
public class ThermostatReadingRepository {

    private final Map<String, List<ThermostatReading>> byDevice;

    @Autowired
    public ThermostatReadingRepository(CsvLoader loader, DataProperties dataProperties) {
        this(loader.load(dataProperties.getSource()));
    }

    ThermostatReadingRepository(List<ThermostatReading> readings) {
        this.byDevice = readings.stream()
                .collect(Collectors.groupingBy(
                        ThermostatReading::getDeviceId,
                        TreeMap::new,
                        Collectors.collectingAndThen(
                                Collectors.toCollection(ArrayList::new),
                                list -> {
                                    list.sort(Comparator.comparing(ThermostatReading::getMeasureTime));
                                    return List.copyOf(list);
                                }
                        )
                ));
    }

    public Set<String> deviceIds() {
        return Collections.unmodifiableSet(byDevice.keySet());
    }

    public boolean exists(String deviceId) {
        return byDevice.containsKey(deviceId);
    }

    public List<ThermostatReading> findAll(String deviceId) {
        return byDevice.getOrDefault(deviceId, List.of());
    }

    /**
     * Returns readings in the half-open interval [from, to).
     * Neither {@code from} nor {@code to} may be null – the caller (service layer) is responsible for resolving defaults.
     */
    public List<ThermostatReading> findInRange(String deviceId, Instant from, Instant to) {
        return findAll(deviceId).stream()
                .filter(r -> !r.getMeasureTime().isBefore(from))
                .filter(r -> r.getMeasureTime().isBefore(to))
                .toList();
    }
}
