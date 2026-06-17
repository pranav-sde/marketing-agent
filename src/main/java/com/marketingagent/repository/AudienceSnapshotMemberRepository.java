package com.marketingagent.repository;

import com.marketingagent.domain.audience.AudienceSnapshotMember;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AudienceSnapshotMemberRepository extends JpaRepository<AudienceSnapshotMember, UUID> {
    List<AudienceSnapshotMember> findByAudienceSnapshot_IdAndEligibleTrue(UUID audienceSnapshotId);

    long countByAudienceSnapshot_IdAndEligibleTrue(UUID audienceSnapshotId);
}
