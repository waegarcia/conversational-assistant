package com.enterprise.assistant.application.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private IntentProcessorService intentProcessorService;

    @Mock
    private WeatherApiClient weatherApiClient;

    @Mock
    private MetricsService metricsService;

    @InjectMocks
    private ConversationService conversationService;

    private ConversationRequest request;
    private Conversation conversation;
    private Timer.Sample timerSample;

    @BeforeEach
    void setUp() {
        request = ConversationRequest.builder()
                .userId("user123")
                .message("Hola")
                .build();

        conversation = Conversation.builder()
                .id(1L)
                .sessionId("test-session-id")
                .userId("user123")
                .status(ConversationStatus.ACTIVE)
                .build();

        timerSample = mock(Timer.Sample.class);
    }

    @Test
    void processMessage() {
        Message assistantMessage = createAssistantMessage("Hola!", Intent.GREETING.name(), null);

        when(metricsService.startTimer()).thenReturn(timerSample);
        when(intentProcessorService.detectIntent(anyString())).thenReturn(Intent.GREETING);
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);
        when(messageRepository.save(any(Message.class))).thenReturn(new Message()).thenReturn(assistantMessage);

        ConversationResponse response = conversationService.processMessage(request);

        assertThat(response).isNotNull();
        assertThat(response.getSessionId()).isEqualTo("test-session-id");
        verify(conversationRepository).save(any(Conversation.class));
        verify(metricsService).incrementConversationsCreated();
    }

    @Test
    void weatherQuery() {
        request.setMessage("Clima en Buenos Aires");
        WeatherResponse weatherResponse = createWeatherResponse();
        Message assistantMessage = createAssistantMessage("El clima en Buenos Aires", Intent.WEATHER_QUERY.name(), "OpenWeather");

        when(metricsService.startTimer()).thenReturn(timerSample);
        when(intentProcessorService.detectIntent(anyString())).thenReturn(Intent.WEATHER_QUERY);
        when(intentProcessorService.extractCity(anyString())).thenReturn("Buenos Aires");
        when(weatherApiClient.getCurrentWeather("Buenos Aires")).thenReturn(weatherResponse);
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);
        when(messageRepository.save(any(Message.class))).thenReturn(new Message()).thenReturn(assistantMessage);

        ConversationResponse response = conversationService.processMessage(request);

        assertThat(response.getExternalServiceUsed()).isEqualTo("OpenWeather");
        verify(weatherApiClient).getCurrentWeather("Buenos Aires");
    }

    @Test
    void weatherApiError() {
        when(metricsService.startTimer()).thenReturn(timerSample);
        when(intentProcessorService.detectIntent(anyString())).thenReturn(Intent.WEATHER_QUERY);
        when(intentProcessorService.extractCity(anyString())).thenReturn("Buenos Aires");
        when(weatherApiClient.getCurrentWeather(anyString())).thenThrow(new RuntimeException("Error"));
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);
        when(messageRepository.save(any(Message.class))).thenReturn(new Message());

        ConversationResponse response = conversationService.processMessage(request);

        assertThat(response).isNotNull();
        verify(metricsService).recordExternalApiFailure();
    }

    @Test
    void endConversation() {
        when(conversationRepository.findBySessionId("test-session-id")).thenReturn(Optional.of(conversation));
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);

        conversationService.endConversation("test-session-id");

        verify(metricsService).decrementActiveConversations();
    }

    private Message createAssistantMessage(String content, String intent, String externalService) {
        return Message.builder()
                .id(1L)
                .conversation(conversation)
                .role(MessageRole.ASSISTANT)
                .content(content)
                .intent(intent)
                .externalServiceUsed(externalService)
                .timestamp(LocalDateTime.now())
                .build();
    }

    private WeatherResponse createWeatherResponse() {
        WeatherResponse response = new WeatherResponse();
        response.setName("Buenos Aires");

        WeatherResponse.Main main = new WeatherResponse.Main();
        main.setTemp(25.0);
        main.setFeelsLike(26.0);
        main.setHumidity(60);
        response.setMain(main);

        WeatherResponse.Weather weather = new WeatherResponse.Weather();
        weather.setMain("Clear");
        weather.setDescription("cielo claro");
        response.setWeather(java.util.List.of(weather));

        WeatherResponse.Wind wind = new WeatherResponse.Wind();
        wind.setSpeed(3.5);
        response.setWind(wind);

        return response;
    }
}
