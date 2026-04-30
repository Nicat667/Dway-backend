package com.dway.dwaybackend.repository;

import com.dway.dwaybackend.entity.Partner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PartnerRepository extends JpaRepository<Partner, UUID> {

    List<Partner> findByIsActiveTrueOrderByCreatedAtDesc();

    Page<Partner> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Partner> findByIsActiveOrderByCreatedAtDesc(boolean isActive, Pageable pageable);

    boolean existsByName(String name);
}