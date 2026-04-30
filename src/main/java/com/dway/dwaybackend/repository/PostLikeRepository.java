package com.dway.dwaybackend.repository;

import com.dway.dwaybackend.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Set;
import java.util.UUID;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, UUID> {

    boolean existsByPostIdAndUserId(UUID postId, UUID userId);

    void deleteByPostIdAndUserId(UUID postId, UUID userId);

    @Query("SELECT pl.postId FROM PostLike pl WHERE pl.userId = :userId AND pl.postId IN :postIds")
    Set<UUID> findLikedPostIds(UUID userId, Set<UUID> postIds);
}