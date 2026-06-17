package com.marketingagent.repository;

import com.marketingagent.domain.audience.Segment;
import com.marketingagent.domain.audience.SegmentType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SegmentRepository extends JpaRepository<Segment, UUID> {
    List<Segment> findByTenant_IdAndActiveTrue(UUID tenantId);

    List<Segment> findByTenant_IdAndTypeAndActiveTrue(UUID tenantId, SegmentType type);
}
