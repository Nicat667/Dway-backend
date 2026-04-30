package com.dway.dwaybackend.repository;

import com.dway.dwaybackend.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    @Query("""
        SELECT c FROM Category c
        WHERE c.userId IS NULL OR c.userId = :userId
        ORDER BY c.isDefault DESC, c.createdAt ASC
        """)
    List<Category> findAllForUser(UUID userId);

    List<Category> findByUserId(UUID userId);

    boolean existsByNameAndUserId(String name, UUID userId);
}

