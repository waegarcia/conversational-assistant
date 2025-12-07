package com.enterprise.assistant.api.controller;

import com.enterprise.assistant.api.dto.ConversationHistoryResponse;
import com.enterprise.assistant.api.dto.ConversationRequest;
import com.enterprise.assistant.api.dto.ConversationResponse;
import com.enterprise.assistant.application.service.ConversationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    @PostMapping
    public ResponseEntity<ConversationResponse> sendMessage(@Valid @RequestBody ConversationRequest request) {
        return ResponseEntity.ok(conversationService.processMessage(request));
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<ConversationHistoryResponse> getConversationHistory(@PathVariable String sessionId) {
        return ResponseEntity.ok(conversationService.getConversationHistoryDto(sessionId));
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> endConversation(@PathVariable String sessionId) {
        conversationService.endConversation(sessionId);
        return ResponseEntity.noContent().build();
    }
}
