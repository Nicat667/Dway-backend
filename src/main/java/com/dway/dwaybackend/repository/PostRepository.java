package com.dway.dwaybackend.repository;

import com.dway.dwaybackend.entity.Post;
import com.dway.dwaybackend.entity.enums.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PostRepository extends JpaRepository<Post, UUID> {

    // CURSOR-BASED — Instagram style infinite scroll
    // First load: pass before = current time → gets newest 20 posts
    // Next loads: pass before = createdAt of last post received → gets next 20
    // No duplicates, no skips when new posts are added while scrolling
    // Used in: GET /mobile/posts?before=...&size=20
    @Query("""
            SELECT p FROM Post p
            WHERE p.isDeleted = false
              AND p.status = 'PUBLISHED'
              AND p.createdAt < :before
            ORDER BY p.createdAt DESC
            LIMIT :size
            """)
    List<Post> findFeedBefore(LocalDateTime before, int size);

    // User's own posts — cursor-based for profile post history scroll
    // User sees their own posts including FLAGGED (not hard deleted)
    // Used in: profile screen infinite scroll
    @Query("""
            SELECT p FROM Post p
            WHERE p.userId = :userId
              AND p.isDeleted = false
              AND p.createdAt < :before
            ORDER BY p.createdAt DESC
            LIMIT :size
            """)
    List<Post> findByUserIdBefore(UUID userId, LocalDateTime before, int size);

    Optional<Post> findByIdAndUserIdAndIsDeletedFalse(UUID id, UUID userId);

    Optional<Post> findByIdAndIsDeletedFalse(UUID id);

    Page<Post> findByIsDeletedFalseOrderByCreatedAtDesc(Pageable pageable);

    Page<Post> findByStatusAndIsDeletedFalseOrderByCreatedAtDesc(PostStatus status, Pageable pageable);

    @Modifying
    @Query("UPDATE Post p SET p.likesCount = p.likesCount + 1 WHERE p.id = :postId")
    void incrementLikesCount(UUID postId);

    @Modifying
    @Query("UPDATE Post p SET p.likesCount = p.likesCount - 1 WHERE p.id = :postId AND p.likesCount > 0")
    void decrementLikesCount(UUID postId);

    @Modifying
    @Query("UPDATE Post p SET p.commentsCount = p.commentsCount + 1 WHERE p.id = :postId")
    void incrementCommentsCount(UUID postId);

    @Modifying
    @Query("UPDATE Post p SET p.commentsCount = p.commentsCount - 1 WHERE p.id = :postId AND p.commentsCount > 0")
    void decrementCommentsCount(UUID postId);

    // Admin dashboard
    @Query("SELECT COUNT(p) FROM Post p WHERE p.isDeleted = false")
    long countActivePosts();
}