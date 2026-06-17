package com.marketingagent.dto.campaign;

import com.marketingagent.domain.campaign.Campaign;
import com.marketingagent.domain.campaign.CampaignStatus;
import java.time.Instant;
import java.util.UUID;

public record CampaignDto(
        UUID id,
        UUID tenantId,
        UUID segmentId,
        String name,
        String objective,
        CampaignStatus status,
        UUID ownerUserId,
        Instant createdAt,
        Instant updatedAt
) {
    public static CampaignDto from(Campaign campaign) {
        return new CampaignDto(
                campaign.getId(),
                campaign.getTenant().getId(),
                campaign.getSegment() == null ? null : campaign.getSegment().getId(),
                campaign.getName(),
                campaign.getObjective(),
                campaign.getStatus(),
                campaign.getOwnerUserId(),
                campaign.getCreatedAt(),
                campaign.getUpdatedAt()
        );
    }
}
