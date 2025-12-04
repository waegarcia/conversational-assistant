package com.enterprise.assistant.domain.repository;

import com.enterprise.assistant.domain.model.Conversation;
import com.enterprise.assistant.domain.model.ConversationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findBySessionId(String sessionId);

    List<Conversation> findByUserId(String userId);

    List<Conversation> findByStatus(ConversationStatus status);

    List<Conversation> findByStartedAtBetween(LocalDateTime start, LocalDateTime end);

    long countByStatus(ConversationStatus status);
}
