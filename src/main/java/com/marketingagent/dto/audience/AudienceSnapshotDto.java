package com.marketingagent.dto.audience;

import com.marketingagent.domain.audience.AudienceSnapshot;
import java.time.Instant;
import java.util.UUID;

public record AudienceSnapshotDto(
        UUID id,
        UUID tenantId,
        UUID segmentId,
        Instant capturedAt,
        long totalCount,
        long eligibleCount,
        long excludedCount
) {
    public static AudienceSnapshotDto from(AudienceSnapshot snapshot) {
        return new AudienceSnapshotDto(
                snapshot.getId(),
                snapshot.getTenant().getId(),
                snapshot.getSegment() == null ? null : snapshot.getSegment().getId(),
                snapshot.getCapturedAt(),
                snapshot.getTotalCount(),
                snapshot.getEligibleCount(),
                snapshot.getExcludedCount()
        );
    }
}
