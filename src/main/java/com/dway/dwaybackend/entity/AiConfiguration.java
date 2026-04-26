package com.dway.dwaybackend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_configurations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "system_prompt", nullable = false, columnDefinition = "TEXT")
    private String systemPrompt;

    @Column(name = "model", nullable = false)
    private String model;

    @Column(name = "max_tokens", nullable = false)
    @Builder.Default
    private int maxTokens = 1000;

    @Column(name = "temperature", nullable = false)
    @Builder.Default
    private double temperature = 0.7;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}