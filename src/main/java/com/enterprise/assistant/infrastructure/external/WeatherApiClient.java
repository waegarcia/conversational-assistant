package com.enterprise.assistant.infrastructure.external;

import com.enterprise.assistant.config.WeatherApiProperties;
import com.enterprise.assistant.infrastructure.external.dto.WeatherResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class WeatherApiClient {

    private final RestTemplate restTemplate;
    private final WeatherApiProperties weatherApiProperties;

    public WeatherResponse getCurrentWeather(String city) {
        try {
            String url = buildWeatherUrl(city);
            return restTemplate.getForObject(url, WeatherResponse.class);
        } catch (Exception e) {
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
