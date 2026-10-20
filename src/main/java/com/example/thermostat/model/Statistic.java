package com.example.thermostat.model;

import com.example.thermostat.exception.InvalidQueryException;

import java.util.Arrays;
import java.util.Locale;
import java.util.OptionalDouble;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

public enum Statistic {
    AVG(DoubleStream::average),
    MIN(DoubleStream::min),
    MAX(DoubleStream::max);

    private final Function<DoubleStream, OptionalDouble> aggregator;

    Statistic(Function<DoubleStream, OptionalDouble> aggregator) {
        this.aggregator = aggregator;
    }

    public OptionalDouble apply(DoubleStream stream) {
        return aggregator.apply(stream);
    }

    public static Statistic parse(String value) {
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        try {
            return Statistic.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            String allowed = Arrays.stream(values())
                    .map(s -> s.name().toLowerCase(Locale.ROOT))
                    .collect(Collectors.joining(", "));
            throw new InvalidQueryException(
                    "Unknown statistic '" + value.trim() + "'. Allowed values: " + allowed);
        }
    }
}