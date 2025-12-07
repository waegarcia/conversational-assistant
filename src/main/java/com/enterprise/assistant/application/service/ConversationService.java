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
import com.enterprise.assistant.domain.model.Intent;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final IntentProcessorService intentProcessorService;
    private final WeatherApiClient weatherApiClient;
    private final MetricsService metricsService;

    @Transactional
    public ConversationResponse processMessage(ConversationRequest request) {
        Timer.Sample sample = metricsService.startTimer();
        metricsService.incrementMessagesProcessed();

        Conversation conversation = getOrCreateConversation(request);

        saveUserMessage(conversation, request.getMessage());

        Intent intent = intentProcessorService.detectIntent(request.getMessage());
        metricsService.recordIntentDetected(intent);

        String responseText = generateResponse(intent, request.getMessage());
        String externalService = intent == Intent.WEATHER_QUERY ? "OpenWeather" : null;

        Message assistantMessage = saveAssistantMessage(
                conversation,
                responseText,
                intent.name(),
                externalService
        );

        metricsService.recordResponseTime(sample, intent);

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
        Conversation conversation = Conversation.builder()
                .sessionId(UUID.randomUUID().toString())
                .userId(userId)
                .status(ConversationStatus.ACTIVE)
                .build();

        metricsService.incrementConversationsCreated();
        return conversationRepository.save(conversation);
    }

    private void saveUserMessage(Conversation conversation, String content) {
        Message userMessage = Message.builder()
                .conversation(conversation)
                .role(MessageRole.USER)
                .content(content)
                .build();

        messageRepository.save(userMessage);
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
            case GREETING -> "Hola! Soy tu asistente virtual. Puedo ayudarte con informacion del clima. En que ciudad te gustaria consultar?";
            case FAREWELL -> "Hasta luego! Que tengas un excelente dia.";
            case HELP -> "Puedo ayudarte a consultar el clima de cualquier ciudad. Solo preguntame algo como: Que tiempo hace en Buenos Aires?";
            case UNKNOWN -> "Disculpa, no entendi tu consulta. Puedo ayudarte con informacion del clima. Preguntame sobre el tiempo en alguna ciudad.";
        };
    }

    private String handleWeatherQuery(String userMessage) {
        try {
            String city = intentProcessorService.extractCity(userMessage);
            metricsService.incrementExternalApiCall();
            WeatherResponse weather = weatherApiClient.getCurrentWeather(city);

            return formatWeatherResponse(weather);

        } catch (Exception e) {
            metricsService.recordExternalApiFailure();
            return "Lo siento, no pude obtener la informacion del clima en este momento. Por favor, intenta nuevamente mas tarde.";
        }
    }

    private String formatWeatherResponse(WeatherResponse weather) {
        String conditions = weather.getWeather().isEmpty() ? "" : 
                "Condiciones: " + weather.getWeather().get(0).getDescription() + "\n";
        
        return String.format("El clima en %s:\nTemperatura: %.1f°C\n%sHumedad: %d%%",
                weather.getName(), weather.getMain().getTemp(), conditions, weather.getMain().getHumidity());
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

    @Transactional
    public void endConversation(String sessionId) {
        Conversation conversation = conversationRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + sessionId));

        conversation.setStatus(ConversationStatus.COMPLETED);
        conversation.setEndedAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        metricsService.decrementActiveConversations();
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
