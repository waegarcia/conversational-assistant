package com.enterprise.assistant.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
@RequiredArgsConstructor
public class RestClientConfig {

    private final WeatherApiProperties weatherApiProperties;

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(weatherApiProperties.getTimeout());
        factory.setReadTimeout(weatherApiProperties.getTimeout());

        return new RestTemplate(factory);
    }
}
