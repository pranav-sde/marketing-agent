package com.marketingagent.dto.broadcast;

import com.marketingagent.domain.broadcast.Broadcast;
import com.marketingagent.domain.broadcast.BroadcastStatus;
import java.time.Instant;
import java.util.UUID;

public record BroadcastDto(
        UUID id,
        UUID tenantId,
        UUID campaignId,
        UUID audienceSnapshotId,
        UUID messageTemplateId,
        UUID templateVersionId,
        UUID whatsAppAccountId,
        BroadcastStatus status,
        Instant scheduledStartAt,
        Instant scheduledEndAt,
        String timezone,
        String rateLimitProfile,
        Instant lockedAt
) {
    public static BroadcastDto from(Broadcast broadcast) {
        return new BroadcastDto(
                broadcast.getId(),
                broadcast.getTenant().getId(),
                broadcast.getCampaign().getId(),
                broadcast.getAudienceSnapshot().getId(),
                broadcast.getMessageTemplate().getId(),
                broadcast.getTemplateVersion().getId(),
                broadcast.getWhatsAppAccount().getId(),
                broadcast.getStatus(),
                broadcast.getScheduledStartAt(),
                broadcast.getScheduledEndAt(),
                broadcast.getTimezone(),
                broadcast.getRateLimitProfile(),
                broadcast.getLockedAt()
        );
    }
}
