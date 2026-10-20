package com.example.thermostat.exception;

import java.util.Set;

public class DeviceNotFoundException extends RuntimeException {

    private final Set<String> unknownIds;

    public DeviceNotFoundException(Set<String> unknownIds) {
        super("Unknown device(s): " + String.join(", ", unknownIds));
        this.unknownIds = unknownIds;
    }

    public Set<String> getUnknownIds() {
        return unknownIds;
    }
}