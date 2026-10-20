package com.example.thermostat.csv;

import com.example.thermostat.model.ThermostatReading;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvLoaderTest {

    private CsvLoader csvLoader;

    @BeforeEach
    void setUp() {
        csvLoader = new CsvLoader();
    }

    private Resource csv(String... rows) {
        String content = "DeviceId,MeasureTime,Temperature,TempUnit,Humidity\n"
                + String.join("\n", rows);
        return new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void celsius_temperature_and_humidity_parsed() {
        List<ThermostatReading> result = csvLoader.load(csv("1,2026-07-10T10:00:00Z,23,C,42"));

        assertEquals(1, result.size());
        assertEquals("1", result.get(0).getDeviceId());
        assertEquals(23.0, result.get(0).getTemperatureCelsius());
        assertEquals(42.0, result.get(0).getHumidity());
    }

    @Test
    void fahrenheit_converted_to_celsius() {
        List<ThermostatReading> result = csvLoader.load(csv("2,2026-07-10T10:00:00Z,77,F,"));

        assertEquals(1, result.size());
        assertEquals(25.0, result.get(0).getTemperatureCelsius());
        assertNull(result.get(0).getHumidity());
    }

    @Test
    void humidity_only_row_parsed() {
        List<ThermostatReading> result = csvLoader.load(csv("1,2026-07-10T10:00:00Z,,,45"));

        assertEquals(1, result.size());
        assertNull(result.get(0).getTemperatureCelsius());
        assertEquals(45.0, result.get(0).getHumidity());
    }

    @Test
    void missing_temperature_unit_skipped() {
        List<ThermostatReading> result = csvLoader.load(csv("1,2026-07-10T10:00:00Z,23,,42"));

        assertTrue(result.isEmpty());
    }

    @Test
    void invalid_date_skipped() {
        List<ThermostatReading> result = csvLoader.load(csv("1,rossz-datum,23,C,42"));

        assertTrue(result.isEmpty());
    }

    @Test
    void nan_temperature_skipped() {
        List<ThermostatReading> result = csvLoader.load(csv("1,2026-07-10T10:00:00Z,NaN,C,42"));

        assertTrue(result.isEmpty());
    }

    @Test
    void humidity_out_of_range_skipped() {
        List<ThermostatReading> result = csvLoader.load(csv("1,2026-07-10T10:00:00Z,23,C,150"));

        assertTrue(result.isEmpty());
    }

    @Test
    void too_few_columns_skipped() {
        List<ThermostatReading> result = csvLoader.load(csv("1,2026-07-10T10:00:00Z,23,C"));

        assertTrue(result.isEmpty());
    }
}