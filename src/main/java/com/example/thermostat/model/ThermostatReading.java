package com.example.thermostat.model;

import java.time.Instant;

public class ThermostatReading {

    private String deviceId;
    private Instant measureTime;
    private Double temperatureCelsius;
    private Double humidity;

    public ThermostatReading(String deviceId, Instant measureTime, Double temperatureCelsius, Double humidity) {
        this.deviceId = deviceId;
        this.measureTime = measureTime;
        this.temperatureCelsius = temperatureCelsius;
        this.humidity = humidity;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public Instant getMeasureTime() {
        return measureTime;
    }

    public Double getTemperatureCelsius() {
        return temperatureCelsius;
    }

    public Double getHumidity() {
        return humidity;
    }
}