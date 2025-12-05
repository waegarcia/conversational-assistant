package com.enterprise.assistant.application.service;

import com.enterprise.assistant.api.dto.ConversationHistoryResponse;
import com.enterprise.assistant.api.dto.ConversationRequest;
import com.enterprise.assistant.api.dto.ConversationResponse;
import com.enterprise.assistant.domain.model.Conversation;
import com.enterprise.assistant.domain.model.ConversationStatus;
import com.enterprise.assistant.domain.model.Message;
import com.enterprise.assistant.domain.model.MessageRole;
import com.enterprise.assistant.domain.repository.ConversationRepository;
import com.enterprise.assistant.domain.repository.MessageRepository;
import com.enterprise.assistant.infrastructure.external.WeatherApiClient;
import com.enterprise.assistant.infrastructure.external.dto.WeatherResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final IntentProcessorService intentProcessorService;
    private final WeatherApiClient weatherApiClient;

    @Transactional
    public ConversationResponse processMessage(ConversationRequest request) {
        log.info("Processing message from user: {}", request.getUserId());

        Conversation conversation = getOrCreateConversation(request);

        saveUserMessage(conversation, request.getMessage());

        Intent intent = intentProcessorService.detectIntent(request.getMessage());
        log.info("Detected intent: {} for message: {}", intent, request.getMessage());

        String responseText = generateResponse(intent, request.getMessage());
        String externalService = intent == Intent.WEATHER_QUERY ? "OpenWeather" : null;

        Message assistantMessage = saveAssistantMessage(
                conversation,
                responseText,
                intent.name(),
                externalService
        );

        return buildResponse(conversation, assistantMessage);
    }

    private Conversation getOrCreateConversation(ConversationRequest request) {
        if (request.getSessionId() != null) {
            return conversationRepository.findBySessionId(request.getSessionId())
                    .orElseGet(() -> createNewConversation(request.getUserId()));
        }
        return createNewConversation(request.getUserId());
    }

    private Conversation createNewConversation(String userId) {
        String sessionId = UUID.randomUUID().toString();
        log.info("Creating new conversation with sessionId: {}", sessionId);

        Conversation conversation = Conversation.builder()
                .sessionId(sessionId)
                .userId(userId)
                .status(ConversationStatus.ACTIVE)
                .build();

        return conversationRepository.save(conversation);
    }

    private void saveUserMessage(Conversation conversation, String content) {
        Message userMessage = Message.builder()
                .conversation(conversation)
                .role(MessageRole.USER)
                .content(content)
                .build();

        messageRepository.save(userMessage);
        log.debug("Saved user message for conversation: {}", conversation.getSessionId());
    }

    private Message saveAssistantMessage(Conversation conversation, String content,
                                         String intent, String externalService) {
        Message assistantMessage = Message.builder()
                .conversation(conversation)
                .role(MessageRole.ASSISTANT)
                .content(content)
                .intent(intent)
                .externalServiceUsed(externalService)
                .build();

        return messageRepository.save(assistantMessage);
    }

    private String generateResponse(Intent intent, String userMessage) {
        return switch (intent) {
            case WEATHER_QUERY -> handleWeatherQuery(userMessage);
            case GREETING -> "¡Hola! Soy tu asistente virtual. Puedo ayudarte con información del clima. ¿En qué ciudad te gustaría consultar?";
            case FAREWELL -> "¡Hasta luego! Que tengas un excelente día.";
            case HELP -> "Puedo ayudarte a consultar el clima de cualquier ciudad. Solo pregúntame algo como: '¿Qué tiempo hace en Buenos Aires?'";
            case UNKNOWN -> "Disculpa, no entendí tu consulta. Puedo ayudarte con información del clima. Pregúntame sobre el tiempo en alguna ciudad.";
        };
    }

    private String handleWeatherQuery(String userMessage) {
        try {
            String city = intentProcessorService.extractCity(userMessage);
            log.info("Extracting weather for city: {}", city);

            WeatherResponse weather = weatherApiClient.getCurrentWeather(city);

            return formatWeatherResponse(weather);

        } catch (Exception e) {
            log.error("Error processing weather query", e);
            return "Lo siento, no pude obtener la información del clima en este momento. Por favor, intenta nuevamente más tarde.";
        }
    }

    private String formatWeatherResponse(WeatherResponse weather) {
        StringBuilder response = new StringBuilder();
        response.append(String.format("El clima en %s:\n", weather.getName()));
        response.append(String.format("Temperatura: %.1f°C (se siente como %.1f°C)\n",
                weather.getMain().getTemp(),
                weather.getMain().getFeelsLike()));

        if (!weather.getWeather().isEmpty()) {
            response.append(String.format("Condiciones: %s\n",
                    weather.getWeather().get(0).getDescription()));
        }

        response.append(String.format("Humedad: %d%%\n", weather.getMain().getHumidity()));
        response.append(String.format("Viento: %.1f m/s", weather.getWind().getSpeed()));

        return response.toString();
    }

    private ConversationResponse buildResponse(Conversation conversation, Message message) {
        return ConversationResponse.builder()
                .sessionId(conversation.getSessionId())
                .message(message.getContent())
                .intent(message.getIntent())
                .timestamp(message.getTimestamp())
                .externalServiceUsed(message.getExternalServiceUsed())
                .conversationActive(conversation.getStatus() == ConversationStatus.ACTIVE)
                .build();
    }

    @Transactional(readOnly = true)
    public Conversation getConversationHistory(String sessionId) {
        return conversationRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + sessionId));
    }

    @Transactional
    public void endConversation(String sessionId) {
        Conversation conversation = conversationRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + sessionId));

        conversation.setStatus(ConversationStatus.COMPLETED);
        conversation.setEndedAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        log.info("Conversation ended: {}", sessionId);
    }

    @Transactional(readOnly = true)
    public ConversationHistoryResponse getConversationHistoryDto(String sessionId) {
        Conversation conversation = conversationRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + sessionId));

        List<ConversationHistoryResponse.MessageDto> messageDtos = conversation.getMessages()
                .stream()
                .map(msg -> ConversationHistoryResponse.MessageDto.builder()
                        .role(msg.getRole().name())
                        .content(msg.getContent())
                        .intent(msg.getIntent())
                        .timestamp(msg.getTimestamp())
                        .externalServiceUsed(msg.getExternalServiceUsed())
                        .build())
                .toList();

        return ConversationHistoryResponse.builder()
                .sessionId(conversation.getSessionId())
                .userId(conversation.getUserId())
                .status(conversation.getStatus())
                .startedAt(conversation.getStartedAt())
                .endedAt(conversation.getEndedAt())
                .messages(messageDtos)
                .build();
    }
}
