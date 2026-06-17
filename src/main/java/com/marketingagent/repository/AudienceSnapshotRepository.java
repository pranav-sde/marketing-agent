package com.marketingagent.repository;

import com.marketingagent.domain.audience.AudienceSnapshot;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AudienceSnapshotRepository extends JpaRepository<AudienceSnapshot, UUID> {
    List<AudienceSnapshot> findByTenant_IdAndSegment_IdOrderByCapturedAtDesc(UUID tenantId, UUID segmentId);
}
