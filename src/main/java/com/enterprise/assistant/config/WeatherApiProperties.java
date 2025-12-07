package com.enterprise.assistant.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "external.weather.api")
@Data
public class WeatherApiProperties {

    private String baseUrl;
    private String key;
    private Integer timeout;
    private String defaultCity;
    private String units;
    private String lang;
}
