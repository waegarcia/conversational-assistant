package com.enterprise.assistant.api.controller;

import com.enterprise.assistant.api.dto.ConversationRequest;
import com.enterprise.assistant.api.dto.ConversationResponse;
import com.enterprise.assistant.application.service.ConversationService;
import com.enterprise.assistant.domain.model.Intent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ConversationController.class)
@ActiveProfiles("test")
class ConversationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ConversationService conversationService;

    @Test
    void processMessageOk() throws Exception {
        ConversationRequest request = ConversationRequest.builder()
                .userId("user123")
                .message("Hola")
                .build();

        ConversationResponse response = ConversationResponse.builder()
                .sessionId("test-session-id")
                .message("¡Hola! Soy tu asistente virtual.")
                .intent(Intent.GREETING.name())
                .timestamp(LocalDateTime.now())
                .conversationActive(true)
                .build();

        when(conversationService.processMessage(any(ConversationRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/conversations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value("test-session-id"))
                .andExpect(jsonPath("$.message").value("¡Hola! Soy tu asistente virtual."))
                .andExpect(jsonPath("$.intent").value(Intent.GREETING.name()))
                .andExpect(jsonPath("$.conversationActive").value(true));

        verify(conversationService, times(1)).processMessage(any(ConversationRequest.class));
    }

    @Test
    void validationErrors() throws Exception {
        mockMvc.perform(post("/api/conversations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Hola\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/conversations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"user123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void handleServiceException() throws Exception {
        when(conversationService.processMessage(any(ConversationRequest.class)))
                .thenThrow(new RuntimeException("Service error"));

        mockMvc.perform(post("/api/conversations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"user123\",\"message\":\"Hola\"}"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void endConversation() throws Exception {
        doNothing().when(conversationService).endConversation("test-session");
        mockMvc.perform(delete("/api/conversations/test-session"))
                .andExpect(status().isNoContent());

        doThrow(new IllegalArgumentException("Not found"))
                .when(conversationService).endConversation("invalid");
        mockMvc.perform(delete("/api/conversations/invalid"))
                .andExpect(status().isBadRequest());
    }
}
