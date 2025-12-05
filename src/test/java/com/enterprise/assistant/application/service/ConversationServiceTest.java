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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

    @InjectMocks
    private ConversationService conversationService;

    private ConversationRequest request;
    private Conversation conversation;

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
    }

    @Test
    @DisplayName("Should create new conversation when sessionId is null")
    void shouldCreateNewConversation() {
        Message assistantMessage = createAssistantMessage(
                "¡Hola! Soy tu asistente virtual.",
                Intent.GREETING.name(),
                null
        );

        when(intentProcessorService.detectIntent(anyString())).thenReturn(Intent.GREETING);
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);
        when(messageRepository.save(any(Message.class)))
                .thenReturn(new Message()) // Primera llamada: mensaje del usuario
                .thenReturn(assistantMessage); // Segunda llamada: mensaje del asistente

        ConversationResponse response = conversationService.processMessage(request);

        assertThat(response).isNotNull();
        assertThat(response.getSessionId()).isEqualTo("test-session-id");
        assertThat(response.getMessage()).isNotNull();
        assertThat(response.getIntent()).isEqualTo(Intent.GREETING.name());
        assertThat(response.isConversationActive()).isTrue();

        verify(conversationRepository, times(1)).save(any(Conversation.class));
        verify(messageRepository, times(2)).save(any(Message.class));
    }

    @Test
    @DisplayName("Should use existing conversation when sessionId is provided")
    void shouldUseExistingConversation() {
        request.setSessionId("test-session-id");

        Message assistantMessage = createAssistantMessage(
                "¡Hola! Soy tu asistente virtual.",
                Intent.GREETING.name(),
                null
        );

        when(conversationRepository.findBySessionId("test-session-id"))
                .thenReturn(Optional.of(conversation));
        when(intentProcessorService.detectIntent(anyString())).thenReturn(Intent.GREETING);
        when(messageRepository.save(any(Message.class)))
                .thenReturn(new Message())
                .thenReturn(assistantMessage);

        ConversationResponse response = conversationService.processMessage(request);

        assertThat(response).isNotNull();
        assertThat(response.getSessionId()).isEqualTo("test-session-id");
        assertThat(response.getMessage()).isNotNull();

        verify(conversationRepository, never()).save(any(Conversation.class));
        verify(conversationRepository, times(1)).findBySessionId("test-session-id");
        verify(messageRepository, times(2)).save(any(Message.class));
    }

    @Test
    @DisplayName("Should handle GREETING intent correctly")
    void shouldHandleGreetingIntent() {
        Message assistantMessage = createAssistantMessage(
                "¡Hola! Soy tu asistente virtual. Puedo ayudarte con información del clima. ¿En qué ciudad te gustaría consultar?",
                Intent.GREETING.name(),
                null
        );

        when(intentProcessorService.detectIntent(anyString())).thenReturn(Intent.GREETING);
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);
        when(messageRepository.save(any(Message.class)))
                .thenReturn(new Message())
                .thenReturn(assistantMessage);

        ConversationResponse response = conversationService.processMessage(request);

        assertThat(response).isNotNull();
        assertThat(response.getMessage()).contains("Hola");
        assertThat(response.getMessage()).contains("asistente virtual");
        assertThat(response.getExternalServiceUsed()).isNull();
    }

    @Test
    @DisplayName("Should handle WEATHER_QUERY intent and call external API")
    void shouldHandleWeatherQueryIntent() {
        request.setMessage("Qué temperatura hace en Buenos Aires");

        WeatherResponse weatherResponse = createWeatherResponse();
        String weatherText = "El clima en Buenos Aires:\nTemperatura: 25.0°C (se siente como 26.0°C)\n" +
                "Condiciones: cielo claro\nHumedad: 60%\nViento: 3.5 m/s";

        Message assistantMessage = createAssistantMessage(
                weatherText,
                Intent.WEATHER_QUERY.name(),
                "OpenWeather"
        );

        when(intentProcessorService.detectIntent(anyString())).thenReturn(Intent.WEATHER_QUERY);
        when(intentProcessorService.extractCity(anyString())).thenReturn("Buenos Aires");
        when(weatherApiClient.getCurrentWeather("Buenos Aires")).thenReturn(weatherResponse);
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);
        when(messageRepository.save(any(Message.class)))
                .thenReturn(new Message())
                .thenReturn(assistantMessage);

        ConversationResponse response = conversationService.processMessage(request);

        assertThat(response).isNotNull();
        assertThat(response.getMessage()).contains("Buenos Aires");
        assertThat(response.getMessage()).contains("25.0°C");
        assertThat(response.getExternalServiceUsed()).isEqualTo("OpenWeather");

        verify(weatherApiClient, times(1)).getCurrentWeather("Buenos Aires");
    }

    @Test
    @DisplayName("Should handle WEATHER_QUERY error gracefully")
    void shouldHandleWeatherApiError() {
        request.setMessage("Qué temperatura hace");

        Message assistantMessage = createAssistantMessage(
                "Lo siento, no pude obtener la información del clima en este momento. Por favor, intenta nuevamente más tarde.",
                Intent.WEATHER_QUERY.name(),
                "OpenWeather"
        );

        when(intentProcessorService.detectIntent(anyString())).thenReturn(Intent.WEATHER_QUERY);
        when(intentProcessorService.extractCity(anyString())).thenReturn("Buenos Aires");
        when(weatherApiClient.getCurrentWeather(anyString())).thenThrow(new RuntimeException("API Error"));
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);
        when(messageRepository.save(any(Message.class)))
                .thenReturn(new Message())
                .thenReturn(assistantMessage);

        ConversationResponse response = conversationService.processMessage(request);

        assertThat(response).isNotNull();
        assertThat(response.getMessage()).contains("no pude obtener");
        assertThat(response.getExternalServiceUsed()).isEqualTo("OpenWeather");
    }

    @Test
    @DisplayName("Should handle HELP intent correctly")
    void shouldHandleHelpIntent() {
        request.setMessage("Ayuda");

        Message assistantMessage = createAssistantMessage(
                "Puedo ayudarte a consultar el clima de cualquier ciudad. Solo pregúntame algo como: '¿Qué tiempo hace en Buenos Aires?'",
                Intent.HELP.name(),
                null
        );

        when(intentProcessorService.detectIntent(anyString())).thenReturn(Intent.HELP);
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);
        when(messageRepository.save(any(Message.class)))
                .thenReturn(new Message())
                .thenReturn(assistantMessage);

        ConversationResponse response = conversationService.processMessage(request);

        assertThat(response).isNotNull();
        assertThat(response.getMessage()).contains("clima");
    }

    @Test
    @DisplayName("Should handle FAREWELL intent correctly")
    void shouldHandleFarewellIntent() {
        request.setMessage("Adiós");

        Message assistantMessage = createAssistantMessage(
                "¡Hasta luego! Que tengas un excelente día.",
                Intent.FAREWELL.name(),
                null
        );

        when(intentProcessorService.detectIntent(anyString())).thenReturn(Intent.FAREWELL);
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);
        when(messageRepository.save(any(Message.class)))
                .thenReturn(new Message())
                .thenReturn(assistantMessage);

        ConversationResponse response = conversationService.processMessage(request);

        assertThat(response).isNotNull();
        assertThat(response.getMessage()).contains("Hasta luego");
    }

    @Test
    @DisplayName("Should handle UNKNOWN intent correctly")
    void shouldHandleUnknownIntent() {
        request.setMessage("xyz123");

        Message assistantMessage = createAssistantMessage(
                "Disculpa, no entendí tu consulta. Puedo ayudarte con información del clima. Pregúntame sobre el tiempo en alguna ciudad.",
                Intent.UNKNOWN.name(),
                null
        );

        when(intentProcessorService.detectIntent(anyString())).thenReturn(Intent.UNKNOWN);
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);
        when(messageRepository.save(any(Message.class)))
                .thenReturn(new Message())
                .thenReturn(assistantMessage);

        ConversationResponse response = conversationService.processMessage(request);

        assertThat(response).isNotNull();
        assertThat(response.getMessage()).contains("no entendí");
    }

    @Test
    @DisplayName("Should end conversation successfully")
    void shouldEndConversation() {
        when(conversationRepository.findBySessionId("test-session-id"))
                .thenReturn(Optional.of(conversation));
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);

        conversationService.endConversation("test-session-id");

        verify(conversationRepository, times(1)).findBySessionId("test-session-id");
        verify(conversationRepository, times(1)).save(any(Conversation.class));
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
