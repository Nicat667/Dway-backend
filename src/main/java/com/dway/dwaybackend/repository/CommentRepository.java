package com.dway.dwaybackend.repository;

import com.dway.dwaybackend.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {

    List<Comment> findByPostIdAndIsDeletedFalseOrderByCreatedAtAsc(UUID postId);

    Optional<Comment> findByIdAndUserIdAndIsDeletedFalse(UUID id, UUID userId);

    Optional<Comment> findByIdAndIsDeletedFalse(UUID id);

    long countByPostIdAndIsDeletedFalse(UUID postId);
}