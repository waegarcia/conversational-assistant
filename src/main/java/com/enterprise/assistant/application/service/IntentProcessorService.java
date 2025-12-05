package com.enterprise.assistant.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
@Slf4j
public class IntentProcessorService {

    private static final Pattern WEATHER_PATTERN = Pattern.compile(
            ".*(clima|temperatura|tiempo|pronóstico|llover|lluvia|frío|calor|grados).*",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern GREETING_PATTERN = Pattern.compile(
            ".*(hola|buenos días|buenas tardes|buenas noches|saludos|qué tal).*",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern FAREWELL_PATTERN = Pattern.compile(
            ".*(adiós|chau|hasta luego|nos vemos|bye).*",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern HELP_PATTERN = Pattern.compile(
            ".*(ayuda|help|qué puedes hacer|cómo funciona|comandos).*",
            Pattern.CASE_INSENSITIVE
    );

    public Intent detectIntent(String message) {
        if (message == null || message.trim().isEmpty()) {
            return Intent.UNKNOWN;
        }

        String normalizedMessage = message.toLowerCase().trim();

        if (GREETING_PATTERN.matcher(normalizedMessage).matches()) {
            log.debug("Detected GREETING intent for message: {}", message);
            return Intent.GREETING;
        }

        if (FAREWELL_PATTERN.matcher(normalizedMessage).matches()) {
            log.debug("Detected FAREWELL intent for message: {}", message);
            return Intent.FAREWELL;
        }

        if (HELP_PATTERN.matcher(normalizedMessage).matches()) {
            log.debug("Detected HELP intent for message: {}", message);
            return Intent.HELP;
        }

        if (WEATHER_PATTERN.matcher(normalizedMessage).matches()) {
            log.debug("Detected WEATHER_QUERY intent for message: {}", message);
            return Intent.WEATHER_QUERY;
        }

        log.debug("No specific intent detected, returning UNKNOWN for message: {}", message);
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

        return "Buenos Aires";
    }
}
