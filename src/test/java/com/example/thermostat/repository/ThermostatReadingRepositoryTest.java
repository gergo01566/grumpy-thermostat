package com.example.thermostat.repository;

import com.example.thermostat.model.ThermostatReading;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ThermostatReadingRepositoryTest {

    private static final Instant T1 = Instant.parse("2026-07-10T10:00:00Z");
    private static final Instant T2 = Instant.parse("2026-07-10T14:00:00Z");
    private static final Instant T3 = Instant.parse("2026-07-10T18:00:00Z");

    private static ThermostatReading reading(String deviceId, Instant time) {
        return new ThermostatReading(deviceId, time, 20.0, 50.0);
    }

    private ThermostatReadingRepository repo;

    @BeforeEach
    void setUp() {
        repo = new ThermostatReadingRepository(List.of(
                reading("1", T3),
                reading("1", T1),
                reading("1", T2),
                reading("2", T2)
        ));
    }

    @Test
    void reversed_input_comes_out_in_time_order() {
        List<ThermostatReading> result = repo.findAll("1");

        assertEquals(List.of(T1, T2, T3), result.stream().map(ThermostatReading::getMeasureTime).toList());
    }

    @Test
    void from_inclusive_to_exclusive() {
        List<ThermostatReading> result = repo.findInRange("1", T1, T3);

        assertEquals(List.of(T1, T2), result.stream().map(ThermostatReading::getMeasureTime).toList());
    }

    @Test
    void empty_interval_returns_empty() {
        List<ThermostatReading> result = repo.findInRange("1", T2, T2);

        assertTrue(result.isEmpty());
    }

    @Test
    void last_reading_included_when_in_range() {
        List<ThermostatReading> result = repo.findInRange("1", T3, Instant.MAX);

        assertEquals(1, result.size());
        assertEquals(T3, result.get(0).getMeasureTime());
    }

    @Test
    void unknown_device_exists_returns_false() {
        assertFalse(repo.exists("99"));
    }

    @Test
    void unknown_device_findAll_returns_empty() {
        assertTrue(repo.findAll("99").isEmpty());
    }

    @Test
    void unknown_device_findInRange_returns_empty() {
        assertTrue(repo.findInRange("99", T1, T3).isEmpty());
    }

    @Test
    void two_devices_data_does_not_leak() {
        assertEquals(3, repo.findAll("1").size());
        assertEquals(1, repo.findAll("2").size());
    }

    @Test
    void deviceIds_are_sorted() {
        repo = new ThermostatReadingRepository(List.of(
                reading("3", T1),
                reading("1", T1),
                reading("2", T1)
        ));

        assertEquals(List.of("1", "2", "3"), List.copyOf(repo.deviceIds()));
    }
}