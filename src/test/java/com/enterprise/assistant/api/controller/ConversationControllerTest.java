package com.enterprise.assistant.api.controller;

import com.enterprise.assistant.api.dto.ConversationRequest;
import com.enterprise.assistant.api.dto.ConversationResponse;
import com.enterprise.assistant.application.service.ConversationService;
import com.enterprise.assistant.application.service.Intent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ConversationController.class)
class ConversationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ConversationService conversationService;

    @Test
    @DisplayName("POST /api/conversations - Should process message successfully")
    void shouldProcessMessageSuccessfully() throws Exception {
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
    @DisplayName("POST /api/conversations - Should return 400 for missing userId")
    void shouldReturn400ForMissingUserId() throws Exception {
        ConversationRequest request = ConversationRequest.builder()
                .message("Hola")
                .build();

        mockMvc.perform(post("/api/conversations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(conversationService, never()).processMessage(any(ConversationRequest.class));
    }

    @Test
    @DisplayName("POST /api/conversations - Should return 400 for missing message")
    void shouldReturn400ForMissingMessage() throws Exception {
        ConversationRequest request = ConversationRequest.builder()
                .userId("user123")
                .build();

        mockMvc.perform(post("/api/conversations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(conversationService, never()).processMessage(any(ConversationRequest.class));
    }

    @Test
    @DisplayName("POST /api/conversations - Should return 400 for empty message")
    void shouldReturn400ForEmptyMessage() throws Exception {
        ConversationRequest request = ConversationRequest.builder()
                .userId("user123")
                .message("   ")
                .build();

        mockMvc.perform(post("/api/conversations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(conversationService, never()).processMessage(any(ConversationRequest.class));
    }

    @Test
    @DisplayName("POST /api/conversations - Should handle service exceptions")
    void shouldHandleServiceExceptions() throws Exception {
        ConversationRequest request = ConversationRequest.builder()
                .userId("user123")
                .message("Hola")
                .build();

        when(conversationService.processMessage(any(ConversationRequest.class)))
                .thenThrow(new RuntimeException("Service error"));

        mockMvc.perform(post("/api/conversations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").exists());

        verify(conversationService, times(1)).processMessage(any(ConversationRequest.class));
    }

    @Test
    @DisplayName("DELETE /api/conversations/{sessionId} - Should end conversation successfully")
    void shouldEndConversationSuccessfully() throws Exception {
        String sessionId = "test-session-id";

        doNothing().when(conversationService).endConversation(sessionId);

        mockMvc.perform(delete("/api/conversations/{sessionId}", sessionId))
                .andExpect(status().isNoContent());

        verify(conversationService, times(1)).endConversation(sessionId);
    }

    @Test
    @DisplayName("DELETE /api/conversations/{sessionId} - Should return 400 for invalid sessionId")
    void shouldReturn400ForInvalidSessionId() throws Exception {
        String sessionId = "invalid-session-id";

        doThrow(new IllegalArgumentException("Conversation not found"))
                .when(conversationService).endConversation(sessionId);

        mockMvc.perform(delete("/api/conversations/{sessionId}", sessionId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").value("Conversation not found"));

        verify(conversationService, times(1)).endConversation(sessionId);
    }
}
