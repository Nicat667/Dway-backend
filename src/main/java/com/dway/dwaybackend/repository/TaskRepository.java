package com.dway.dwaybackend.repository;

import com.dway.dwaybackend.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {

    @Query("""
            SELECT t FROM Task t
            WHERE t.userId = :userId AND t.isDeleted = false
            ORDER BY t.createdAt DESC
            """)
    List<Task> findAllActiveByUserId(UUID userId);

    @Query("""
            SELECT t FROM Task t
            WHERE t.userId = :userId
              AND t.period = :period
              AND t.isDeleted = false
            """)
    List<Task> findByUserIdAndPeriod(UUID userId, String period);

    @Query("""
            SELECT t FROM Task t
            WHERE t.userId = :userId
              AND t.categoryId = :categoryId
              AND t.isDeleted = false
            """)
    List<Task> findByUserIdAndCategoryId(UUID userId, UUID categoryId);

    @Query("""
            SELECT t FROM Task t
            WHERE t.userId = :userId AND t.updatedAt > :since
            """)
    List<Task> findByUserIdUpdatedAfter(UUID userId, LocalDateTime since);

    Optional<Task> findByIdAndUserIdAndIsDeletedFalse(UUID id, UUID userId);

    @Query("""
            SELECT COUNT(t) 
            FROM Task t 
            WHERE t.userId = :userId 
              AND t.isCompleted = true 
              AND t.isDeleted = false
            """)
    long countCompletedByUserId(UUID userId);

    @Query("""
            SELECT COUNT(t) 
            FROM Task t 
            WHERE t.userId = :userId AND t.isDeleted = false
            """)
    long countActiveByUserId(UUID userId);
}