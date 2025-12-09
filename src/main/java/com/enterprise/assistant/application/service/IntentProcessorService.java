package com.enterprise.assistant.application.service;

import com.enterprise.assistant.config.WeatherApiProperties;
import com.enterprise.assistant.domain.model.Intent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class IntentProcessorService {

    private static final Logger log = LoggerFactory.getLogger(IntentProcessorService.class);

    private final WeatherApiProperties weatherApiProperties;

    private static final Pattern WEATHER_PATTERN = Pattern.compile(
            ".*(clima|temperatura|tiempo|pronóstico|llover|lluvia|frío|calor|grados).*",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern GREETING_PATTERN = Pattern.compile(
            ".*(hola|buenos días|buenas tardes|buenas noches|saludos|qué tal).*",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern FAREWELL_PATTERN = Pattern.compile(
            ".*(adiós|chau|hasta luego|nos vemos|bye).*",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern HELP_PATTERN = Pattern.compile(
            ".*(ayuda|help|qué puedes hacer|cómo funciona|comandos).*",
            Pattern.CASE_INSENSITIVE);

    public Intent detectIntent(String message) {
        if (message == null || message.trim().isEmpty()) {
            return Intent.UNKNOWN;
        }
        String msg = message.toLowerCase().trim();

        if (GREETING_PATTERN.matcher(msg).matches()) return Intent.GREETING;
        if (FAREWELL_PATTERN.matcher(msg).matches()) return Intent.FAREWELL;
        if (HELP_PATTERN.matcher(msg).matches()) return Intent.HELP;
        if (WEATHER_PATTERN.matcher(msg).matches()) return Intent.WEATHER_QUERY;

        return Intent.UNKNOWN;
    }

    // TODO: usar librería de NLP o diccionario de ciudades para mejorar detección
    public String extractCity(String message) {
        String[] commonPrepositions = {"en", "de", "para", "desde"};
        String lowerMessage = message.toLowerCase();

        for (String prep : commonPrepositions) {
            int index = lowerMessage.indexOf(prep + " ");
            if (index != -1) {
                String afterPrep = message.substring(index + prep.length() + 1).trim();

                String[] words = afterPrep.split("[\\s,?.!]+");

                // Detección básica de ciudades compuestas comunes en español
                // Ej: Buenos Aires, San Francisco, Santa Fe, Los Angeles, El Cairo, La Paz...
                if (words.length >= 2 &&
                        (words[0].equalsIgnoreCase("buenos") ||
                                words[0].equalsIgnoreCase("san") ||
                                words[0].equalsIgnoreCase("santa") ||
                                words[0].equalsIgnoreCase("los") ||
                                words[0].equalsIgnoreCase("el") ||
                                words[0].equalsIgnoreCase("la"))) {
                    String cityName = words[0] + " " + words[1];
                    log.debug("Extracted compound city name: {}", cityName);
                    return cityName;
                }

                if (words.length > 0) {
                    String cityName = words[0].replaceAll("[^a-zA-ZáéíóúñÁÉÍÓÚÑ]", "");
                    log.debug("Extracted single-word city name: {}", cityName);
                    return cityName;
                }
            }
        }

        log.debug("Could not extract city from message, using default: {}",
                weatherApiProperties.getDefaultCity());
        return weatherApiProperties.getDefaultCity();
    }
}
