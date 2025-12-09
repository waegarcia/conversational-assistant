package com.enterprise.assistant.infrastructure.external;

import com.enterprise.assistant.config.WeatherApiProperties;
import com.enterprise.assistant.infrastructure.external.dto.WeatherResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class WeatherApiClient {

    private static final Logger log = LoggerFactory.getLogger(WeatherApiClient.class);

    private final RestTemplate restTemplate;
    private final WeatherApiProperties weatherApiProperties;

    public WeatherResponse getCurrentWeather(String city) {
        try {
            log.info("Fetching weather data for city: {}", city);
            String url = buildWeatherUrl(city);
            WeatherResponse response = restTemplate.getForObject(url, WeatherResponse.class);
            log.debug("Weather retrieved successfully for city: {}", city);
            return response;
        } catch (Exception e) {
            log.error("Failed to fetch weather for city: {}. Error: {}", city, e.getMessage());
            throw new ExternalServiceException("Failed to retrieve weather data", e);
        }
    }

    private String buildWeatherUrl(String city) {
        return UriComponentsBuilder
                .fromUriString(weatherApiProperties.getBaseUrl())
                .path("/weather")
                .queryParam("q", city)
                .queryParam("appid", weatherApiProperties.getKey())
                .queryParam("units", weatherApiProperties.getUnits())
                .queryParam("lang", weatherApiProperties.getLang())
                .build()
                .toUriString();
    }
}
