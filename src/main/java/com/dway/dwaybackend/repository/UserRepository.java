package com.dway.dwaybackend.repository;

import com.dway.dwaybackend.entity.User;
import com.dway.dwaybackend.entity.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r = :role")
    List<User> findAllByRole(Role role);
}