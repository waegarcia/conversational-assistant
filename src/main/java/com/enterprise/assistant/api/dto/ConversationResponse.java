package com.enterprise.assistant.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponse {

    private String sessionId;
    private String message;
    private String intent;
    private LocalDateTime timestamp;
    private String externalServiceUsed;
    private boolean conversationActive;
}
