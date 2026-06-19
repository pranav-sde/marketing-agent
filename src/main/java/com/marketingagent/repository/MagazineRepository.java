package com.marketingagent.repository;

import com.marketingagent.domain.magazine.Magazine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MagazineRepository extends JpaRepository<Magazine, UUID> {
    List<Magazine> findByTenant_IdOrderByCreatedAtDesc(UUID tenantId);
}
