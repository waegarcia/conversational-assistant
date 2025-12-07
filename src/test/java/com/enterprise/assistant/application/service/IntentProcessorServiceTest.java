package com.enterprise.assistant.application.service;

import com.enterprise.assistant.config.WeatherApiProperties;
import com.enterprise.assistant.domain.model.Intent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IntentProcessorServiceTest {

    private IntentProcessorService intentProcessorService;

    @BeforeEach
    void setUp() {
        WeatherApiProperties props = new WeatherApiProperties();
        props.setDefaultCity("Buenos Aires");
        intentProcessorService = new IntentProcessorService(props);
    }

    @Test
    void detectIntents() {
        assertThat(intentProcessorService.detectIntent("Hola")).isEqualTo(Intent.GREETING);
        assertThat(intentProcessorService.detectIntent("HOLA")).isEqualTo(Intent.GREETING);
        assertThat(intentProcessorService.detectIntent("Qué clima hace")).isEqualTo(Intent.WEATHER_QUERY);
        assertThat(intentProcessorService.detectIntent("Adiós")).isEqualTo(Intent.FAREWELL);
        assertThat(intentProcessorService.detectIntent("Ayuda")).isEqualTo(Intent.HELP);
        assertThat(intentProcessorService.detectIntent("xyz123")).isEqualTo(Intent.UNKNOWN);
    }

    @Test
    void handleNullAndEmpty() {
        assertThat(intentProcessorService.detectIntent(null)).isEqualTo(Intent.UNKNOWN);
        assertThat(intentProcessorService.detectIntent("   ")).isEqualTo(Intent.UNKNOWN);
    }

    @Test
    void extractCity() {
        assertThat(intentProcessorService.extractCity("Clima en Buenos Aires")).isEqualTo("Buenos Aires");
        assertThat(intentProcessorService.extractCity("Clima en Córdoba")).isEqualTo("Córdoba");
        assertThat(intentProcessorService.extractCity("Qué temperatura hace")).isEqualTo("Buenos Aires");
    }
}
