package com.enterprise.assistant.application.service;

import com.enterprise.assistant.config.WeatherApiProperties;
import com.enterprise.assistant.domain.model.Intent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class IntentProcessorService {

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

    public String extractCity(String message) {
        String[] commonPrepositions = {"en", "de", "para", "desde"};
        String lowerMessage = message.toLowerCase();

        for (String prep : commonPrepositions) {
            int index = lowerMessage.indexOf(prep + " ");
            if (index != -1) {
                String afterPrep = message.substring(index + prep.length() + 1).trim();

                String[] words = afterPrep.split("[\\s,?.!]+");

                if (words.length >= 2 &&
                        (words[0].equalsIgnoreCase("buenos") ||
                                words[0].equalsIgnoreCase("san") ||
                                words[0].equalsIgnoreCase("santa"))) {
                    return words[0] + " " + words[1];
                }

                if (words.length > 0) {
                    return words[0].replaceAll("[^a-zA-ZáéíóúñÁÉÍÓÚÑ]", "");
                }
            }
        }

        return weatherApiProperties.getDefaultCity();
    }
}
