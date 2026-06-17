package com.marketingagent.dto.audience;

import com.marketingagent.domain.audience.Segment;
import com.marketingagent.domain.audience.SegmentType;
import java.util.Map;
import java.util.UUID;

public record SegmentDto(
        UUID id,
        UUID tenantId,
        String name,
        SegmentType type,
        Map<String, Object> ruleDefinition,
        boolean active
) {
    public static SegmentDto from(Segment segment) {
        return new SegmentDto(
                segment.getId(),
                segment.getTenant().getId(),
                segment.getName(),
                segment.getType(),
                segment.getRuleDefinition(),
                segment.isActive()
        );
    }
}
