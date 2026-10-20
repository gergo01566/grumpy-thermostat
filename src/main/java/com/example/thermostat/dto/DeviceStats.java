package com.example.thermostat.dto;

import java.util.Map;

public record DeviceStats(String deviceId, Map<String, Double> metrics) {}