package com.example.thermostat.model;

import com.example.thermostat.exception.InvalidQueryException;

import java.util.Arrays;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum Metric {
    TEMPERATURE(ThermostatReading::getTemperatureCelsius),
    HUMIDITY(ThermostatReading::getHumidity);

    private final Function<ThermostatReading, Double> getter;

    Metric(Function<ThermostatReading, Double> getter) {
        this.getter = getter;
    }

    public Function<ThermostatReading, Double> getter() {
        return getter;
    }

    public static Metric parse(String value) {
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        try {
            return Metric.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            String allowed = Arrays.stream(values())
                    .map(m -> m.name().toLowerCase(Locale.ROOT))
                    .collect(Collectors.joining(", "));
            throw new InvalidQueryException(
                    "Unknown metric '" + value.trim() + "'. Allowed values: " + allowed);
        }
    }
}