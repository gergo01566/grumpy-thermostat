package com.example.thermostat.csv;

import com.example.thermostat.model.TempUnit;
import com.example.thermostat.model.ThermostatReading;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Component
public class CsvLoader {

    private static final Logger log = LoggerFactory.getLogger(CsvLoader.class);

    public List<ThermostatReading> load(Resource resource) {
        List<ThermostatReading> readings = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (lineNumber == 1) continue; // header
                if (line.isBlank()) continue;

                ThermostatReading reading = parseLine(line, lineNumber);
                if (reading != null) {
                    readings.add(reading);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load CSV: " + resource.getDescription(), e);
        }

        log.info("Loaded {} readings from {}", readings.size(), resource.getDescription());
        return readings;
    }

    private ThermostatReading parseLine(String line, int lineNumber) {
        String[] parts = line.split(",", -1);
        if (parts.length != 5) {
            log.warn("Line {}: skipping malformed line: {}", lineNumber, line);
            return null;
        }

        try {
            String deviceId = parts[0].trim();
            if (deviceId.isEmpty()) throw new IllegalArgumentException("deviceId is blank");

            Instant measureTime = Instant.parse(parts[1].trim());

            Double temperatureCelsius = null;
            if (!parts[2].isBlank()) {
                double rawTemp = Double.parseDouble(parts[2].trim());
                if (!Double.isFinite(rawTemp)) throw new IllegalArgumentException("temperature is not finite: " + rawTemp);
                TempUnit tempUnit;
                try {
                    tempUnit = TempUnit.valueOf(parts[3].trim().toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("unknown or missing temperature unit: '" + parts[3] + "'");
                }
                temperatureCelsius = (tempUnit == TempUnit.F) ? (rawTemp - 32) * 5.0 / 9.0 : rawTemp;
            }

            Double humidity = null;
            if (!parts[4].isBlank()) {
                humidity = Double.parseDouble(parts[4].trim());
                if (!Double.isFinite(humidity)) throw new IllegalArgumentException("humidity is not finite: " + humidity);
                if (humidity < 0 || humidity > 100) throw new IllegalArgumentException("humidity out of range [0, 100]: " + humidity);
            }

            if (temperatureCelsius == null && humidity == null) throw new IllegalArgumentException("no metrics in row");

            return new ThermostatReading(deviceId, measureTime, temperatureCelsius, humidity);
        } catch (DateTimeParseException | IllegalArgumentException e) {
            log.warn("Line {}: skipping invalid line '{}': {}", lineNumber, line, e.getMessage());
            return null;
        }
    }
}