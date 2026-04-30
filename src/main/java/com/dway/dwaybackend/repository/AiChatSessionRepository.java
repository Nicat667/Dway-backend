package com.dway.dwaybackend.repository;

import com.dway.dwaybackend.entity.AiChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AiChatSessionRepository extends JpaRepository<AiChatSession, UUID> {

    List<AiChatSession> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<AiChatSession> findByIdAndUserId(UUID id, UUID userId);
}