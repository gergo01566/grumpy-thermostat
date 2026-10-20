package com.example.thermostat;

import com.example.thermostat.config.DataProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(DataProperties.class)
public class GrumpyThermostatApplication {

	public static void main(String[] args) {
		SpringApplication.run(GrumpyThermostatApplication.class, args);
	}

}
