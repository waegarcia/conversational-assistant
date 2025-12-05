package com.enterprise.assistant.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IntentProcessorServiceTest {

    private IntentProcessorService intentProcessorService;

    @BeforeEach
    void setUp() {
        intentProcessorService = new IntentProcessorService();
    }

    @Test
    @DisplayName("Should detect GREETING intent for 'Hola'")
    void shouldDetectGreetingIntent() {
        Intent result = intentProcessorService.detectIntent("Hola");
        assertThat(result).isEqualTo(Intent.GREETING);
    }

    @Test
    @DisplayName("Should detect GREETING intent for 'Buenos días'")
    void shouldDetectGreetingWithBuenosDias() {
        Intent result = intentProcessorService.detectIntent("Buenos días");
        assertThat(result).isEqualTo(Intent.GREETING);
    }

    @Test
    @DisplayName("Should detect WEATHER_QUERY intent for climate question")
    void shouldDetectWeatherQueryIntent() {
        Intent result = intentProcessorService.detectIntent("¿Qué temperatura hace?");
        assertThat(result).isEqualTo(Intent.WEATHER_QUERY);
    }

    @Test
    @DisplayName("Should detect WEATHER_QUERY intent for clima keyword")
    void shouldDetectWeatherQueryWithClima() {
        Intent result = intentProcessorService.detectIntent("Cómo está el clima");
        assertThat(result).isEqualTo(Intent.WEATHER_QUERY);
    }

    @Test
    @DisplayName("Should detect FAREWELL intent for 'Adiós'")
    void shouldDetectFarewellIntent() {
        Intent result = intentProcessorService.detectIntent("Adiós");
        assertThat(result).isEqualTo(Intent.FAREWELL);
    }

    @Test
    @DisplayName("Should detect HELP intent for 'ayuda'")
    void shouldDetectHelpIntent() {
        Intent result = intentProcessorService.detectIntent("Necesito ayuda");
        assertThat(result).isEqualTo(Intent.HELP);
    }

    @Test
    @DisplayName("Should return UNKNOWN intent for unrecognized message")
    void shouldReturnUnknownIntent() {
        Intent result = intentProcessorService.detectIntent("xyz123");
        assertThat(result).isEqualTo(Intent.UNKNOWN);
    }

    @Test
    @DisplayName("Should return UNKNOWN intent for null message")
    void shouldReturnUnknownForNullMessage() {
        Intent result = intentProcessorService.detectIntent(null);
        assertThat(result).isEqualTo(Intent.UNKNOWN);
    }

    @Test
    @DisplayName("Should return UNKNOWN intent for empty message")
    void shouldReturnUnknownForEmptyMessage() {
        Intent result = intentProcessorService.detectIntent("   ");
        assertThat(result).isEqualTo(Intent.UNKNOWN);
    }

    @Test
    @DisplayName("Should extract 'Buenos Aires' from message")
    void shouldExtractBuenosAires() {
        String result = intentProcessorService.extractCity("Qué temperatura hace en Buenos Aires");
        assertThat(result).isEqualTo("Buenos Aires");
    }

    @Test
    @DisplayName("Should extract 'Córdoba' from message")
    void shouldExtractCordoba() {
        String result = intentProcessorService.extractCity("Cómo está el clima en Córdoba");
        assertThat(result).isEqualTo("Córdoba");
    }

    @Test
    @DisplayName("Should extract 'San Francisco' as compound city name")
    void shouldExtractSanFrancisco() {
        String result = intentProcessorService.extractCity("Temperatura en San Francisco");
        assertThat(result).isEqualTo("San Francisco");
    }

    @Test
    @DisplayName("Should return default city when no city mentioned")
    void shouldReturnDefaultCity() {
        String result = intentProcessorService.extractCity("Qué temperatura hace");
        assertThat(result).isEqualTo("Buenos Aires");
    }

    @Test
    @DisplayName("Should be case insensitive for intent detection")
    void shouldBeCaseInsensitive() {
        Intent result1 = intentProcessorService.detectIntent("HOLA");
        Intent result2 = intentProcessorService.detectIntent("hola");
        Intent result3 = intentProcessorService.detectIntent("HoLa");

        assertThat(result1).isEqualTo(Intent.GREETING);
        assertThat(result2).isEqualTo(Intent.GREETING);
        assertThat(result3).isEqualTo(Intent.GREETING);
    }
}
