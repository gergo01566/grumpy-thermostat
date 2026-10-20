package com.example.thermostat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

@ConfigurationProperties(prefix = "app.data")
public class DataProperties {

    private Resource source;

    public Resource getSource() {
        return source;
    }

    public void setSource(Resource source) {
        this.source = source;
    }
}