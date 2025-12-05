package com.enterprise.assistant.infrastructure.external;

import com.enterprise.assistant.config.WeatherApiProperties;
import com.enterprise.assistant.infrastructure.external.dto.WeatherResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
@Slf4j
public class WeatherApiClient {

    private final RestTemplate restTemplate;
    private final WeatherApiProperties weatherApiProperties;

    public WeatherResponse getCurrentWeather(String city) {
        try {
            String url = buildWeatherUrl(city);
            log.info("Calling OpenWeather API for city: {}", city);

            WeatherResponse response = restTemplate.getForObject(url, WeatherResponse.class);

            log.info("Successfully retrieved weather data for: {}", city);
            return response;

        } catch (Exception e) {
            log.error("Error calling OpenWeather API for city: {}", city, e);
            throw new ExternalServiceException("Failed to retrieve weather data", e);
        }
    }

    private String buildWeatherUrl(String city) {
        return UriComponentsBuilder
                .fromUriString(weatherApiProperties.getBaseUrl())
                .path("/weather")
                .queryParam("q", city)
                .queryParam("appid", weatherApiProperties.getKey())
                .queryParam("units", "metric")
                .queryParam("lang", "es")
                .build()
                .toUriString();
    }
}
