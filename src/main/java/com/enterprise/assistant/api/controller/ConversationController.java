package com.enterprise.assistant.api.controller;

import com.enterprise.assistant.api.dto.ConversationHistoryResponse;
import com.enterprise.assistant.api.dto.ConversationRequest;
import com.enterprise.assistant.api.dto.ConversationResponse;
import com.enterprise.assistant.application.service.ConversationService;
import com.enterprise.assistant.domain.model.Conversation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Conversations", description = "Conversational Assistant API")
public class ConversationController {

    private final ConversationService conversationService;

    @PostMapping
    @Operation(summary = "Send a message", description = "Process a user message and get assistant response")
    public ResponseEntity<ConversationResponse> sendMessage(@Valid @RequestBody ConversationRequest request) {
        log.info("Received message request from user: {}", request.getUserId());

        ConversationResponse response = conversationService.processMessage(request);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{sessionId}")
    @Operation(summary = "Get conversation history", description = "Retrieve complete conversation history by session ID")
    public ResponseEntity<ConversationHistoryResponse> getConversationHistory(@PathVariable String sessionId) {
        log.info("Retrieving conversation history for session: {}", sessionId);

        ConversationHistoryResponse response = conversationService.getConversationHistoryDto(sessionId);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{sessionId}")
    @Operation(summary = "End conversation", description = "Mark a conversation as completed")
    public ResponseEntity<Void> endConversation(@PathVariable String sessionId) {
        log.info("Ending conversation: {}", sessionId);

        conversationService.endConversation(sessionId);

        return ResponseEntity.noContent().build();
    }
}
