package com.example.thermostat.repository;

import com.example.thermostat.exception.DeviceNotFoundException;
import com.example.thermostat.exception.InvalidQueryException;
import com.example.thermostat.model.Metric;
import com.example.thermostat.model.Statistic;
import com.example.thermostat.model.ThermostatReading;
import com.example.thermostat.service.ReadingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ReadingServiceTest {

    // Device 1 timestamps
    private static final Instant T1 = Instant.parse("2026-07-10T10:00:00Z");
    private static final Instant T2 = Instant.parse("2026-07-10T14:00:00Z");
    private static final Instant T3 = Instant.parse("2026-07-10T18:00:00Z");
    private static final Instant T4 = Instant.parse("2026-07-11T10:00:00Z");
    // Device 2 timestamps
    private static final Instant D2_T1 = Instant.parse("2026-07-10T10:00:00Z");
    private static final Instant D2_T2 = Instant.parse("2026-07-10T12:00:00Z");

    // Full range covering all data
    private static final Instant FROM_ALL = Instant.parse("2026-07-10T00:00:00Z");
    private static final Instant TO_ALL   = Instant.parse("2026-07-12T00:00:00Z");

    private ReadingService service;

    @BeforeEach
    void setUp() {
        // Temperatures already in Celsius (as CsvLoader would store them)
        List<ThermostatReading> readings = List.of(
                new ThermostatReading("1", T1,    23.0, 42.0),
                new ThermostatReading("1", T2,    32.0, 45.0),
                new ThermostatReading("1", T3,    20.0, 40.0),
                new ThermostatReading("1", T4,    22.0, 44.0),
                new ThermostatReading("2", D2_T1, 25.0, null),              // 77°F
                new ThermostatReading("2", D2_T2, (80 - 32) * 5.0 / 9.0, null)  // 80°F = 26.666...°C
        );
        service = new ReadingService(new ThermostatReadingRepository(readings));
    }

    @Test
    void device1_full_period_avg_temperature_and_humidity() {
        var result = service.query(Set.of("1"), FROM_ALL, TO_ALL, null, Statistic.AVG);

        var metrics = result.devices().get(0).metrics();
        assertEquals(24.25, metrics.get("temperature"));
        assertEquals(42.75, metrics.get("humidity"));
    }

    @Test
    void device2_full_period_avg_temperature_humidity_null() {
        var result = service.query(Set.of("2"), FROM_ALL, TO_ALL, null, Statistic.AVG);

        var metrics = result.devices().get(0).metrics();
        assertEquals(25.83, metrics.get("temperature"));
        assertNull(metrics.get("humidity"));
    }

    @Test
    void device1_min_max_temperature() {
        var min = service.query(Set.of("1"), FROM_ALL, TO_ALL, Set.of(Metric.TEMPERATURE), Statistic.MIN);
        var max = service.query(Set.of("1"), FROM_ALL, TO_ALL, Set.of(Metric.TEMPERATURE), Statistic.MAX);

        assertEquals(20.0, min.devices().get(0).metrics().get("temperature"));
        assertEquals(32.0, max.devices().get(0).metrics().get("temperature"));
    }

    @Test
    void device1_latest_returns_last_reading() {
        var result = service.query(Set.of("1"), null, null, null, null);

        var metrics = result.devices().get(0).metrics();
        assertEquals(22.0, metrics.get("temperature"));
        assertEquals(44.0, metrics.get("humidity"));
    }

    @Test
    void device2_latest_humidity_null() {
        var result = service.query(Set.of("2"), null, null, null, null);

        var metrics = result.devices().get(0).metrics();
        assertEquals(26.67, metrics.get("temperature"));
        assertNull(metrics.get("humidity"));
    }

    @Test
    void half_open_interval_excludes_to_boundary() {
        Instant from = Instant.parse("2026-07-10T10:00:00Z");
        Instant to   = Instant.parse("2026-07-10T14:00:00Z");

        var result = service.query(Set.of("1"), from, to, null, Statistic.AVG);

        var metrics = result.devices().get(0).metrics();
        assertEquals(23.0, metrics.get("temperature"));
        assertEquals(42.0, metrics.get("humidity"));
    }

    @Test
    void unknown_device_throws_DeviceNotFoundException() {
        assertThrows(DeviceNotFoundException.class,
                () -> service.query(Set.of("99"), FROM_ALL, TO_ALL, null, null));
    }

    @Test
    void one_known_one_unknown_throws_DeviceNotFoundException_for_unknown() {
        var ex = assertThrows(DeviceNotFoundException.class,
                () -> service.query(Set.of("1", "99"), FROM_ALL, TO_ALL, null, null));

        assertTrue(ex.getUnknownIds().contains("99"));
        assertFalse(ex.getUnknownIds().contains("1"));
    }

    @Test
    void only_from_throws_InvalidQueryException() {
        assertThrows(InvalidQueryException.class,
                () -> service.query(null, FROM_ALL, null, null, null));
    }

    @Test
    void from_equals_to_throws_InvalidQueryException() {
        assertThrows(InvalidQueryException.class,
                () -> service.query(null, FROM_ALL, FROM_ALL, null, null));
    }

    @Test
    void interval_with_no_data_returns_null_values() {
        Instant from = Instant.parse("2020-01-01T00:00:00Z");
        Instant to   = Instant.parse("2020-01-02T00:00:00Z");

        var result = service.query(Set.of("1"), from, to, null, Statistic.AVG);

        var metrics = result.devices().get(0).metrics();
        assertNull(metrics.get("temperature"));
        assertNull(metrics.get("humidity"));
    }
}
