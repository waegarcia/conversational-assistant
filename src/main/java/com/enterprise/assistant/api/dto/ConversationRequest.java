package com.enterprise.assistant.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationRequest {

    private String sessionId;

    @NotBlank(message = "User ID is required")
    @Size(max = 100)
    private String userId;

    @NotBlank(message = "Message cannot be empty")
    @Size(max = 2000)
    private String message;
}
