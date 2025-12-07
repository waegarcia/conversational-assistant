package com.enterprise.assistant.api.dto;

import com.enterprise.assistant.domain.model.ConversationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationHistoryResponse {

    private String sessionId;
    private String userId;
    private ConversationStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private List<MessageDto> messages;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageDto {
        private String role;
        private String content;
        private String intent;
        private LocalDateTime timestamp;
        private String externalServiceUsed;
    }
}
