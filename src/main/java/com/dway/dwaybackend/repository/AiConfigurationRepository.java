package com.dway.dwaybackend.repository;

import com.dway.dwaybackend.entity.AiConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AiConfigurationRepository extends JpaRepository<AiConfiguration, UUID> {

    // Single row — always fetch the first (and only) one
    // Used in: AiChatService before every AI call
    // Used in: GET /admin/ai/config
    @Query("SELECT a FROM AiConfiguration a ORDER BY a.updatedAt DESC LIMIT 1")
    Optional<AiConfiguration> findGlobal();
}