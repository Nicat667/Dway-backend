package com.dway.dwaybackend.repository;

import com.dway.dwaybackend.entity.AiChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AiChatMessageRepository extends JpaRepository<AiChatMessage, UUID> {

    // Load ALL messages in a session, oldest first
    // This is the full conversation history sent to the AI API as context
    // Oldest first = chronological order — AI reads conversation top to bottom
    // NO pagination — AI needs the complete history, not just recent messages
    // Used in: POST /mobile/ai/chat — before every new message
    // Used in: GET /mobile/ai/sessions/{id} — show conversation to user
    List<AiChatMessage> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);

    // Bulk delete when a session is deleted
    // @Modifying required — custom DELETE query (DML)
    // Single SQL statement instead of loading all messages then deleting one by one
    @Modifying
    @Query("DELETE FROM AiChatMessage m WHERE m.sessionId = :sessionId")
    void deleteBySessionId(UUID sessionId);
}