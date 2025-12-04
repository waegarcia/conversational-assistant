package com.enterprise.assistant.domain.repository;

import com.enterprise.assistant.domain.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversationIdOrderByTimestampAsc(Long conversationId);

    @Query("SELECT m.intent, COUNT(m) FROM Message m WHERE m.intent IS NOT NULL GROUP BY m.intent")
    List<Object[]> countMessagesByIntent();

    @Query("SELECT m.externalServiceUsed, COUNT(m) FROM Message m WHERE m.externalServiceUsed IS NOT NULL GROUP BY m.externalServiceUsed")
    List<Object[]> countMessagesByExternalService();
}
