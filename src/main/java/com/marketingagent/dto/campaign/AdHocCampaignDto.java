package com.marketingagent.dto.campaign;

import com.marketingagent.domain.campaign.AdHocCampaign;
import com.marketingagent.domain.campaign.AdHocCampaignStatus;
import com.marketingagent.domain.magazine.ContentPlatform;

import java.time.Instant;
import java.util.UUID;

public record AdHocCampaignDto(
        UUID id,
        String messageText,
        String mediaUrl,
        ContentPlatform platform,
        Instant scheduledTime,
        AdHocCampaignStatus status
) {
    public static AdHocCampaignDto from(AdHocCampaign campaign) {
        return new AdHocCampaignDto(
                campaign.getId(),
                campaign.getMessageText(),
                campaign.getMediaUrl(),
                campaign.getPlatform(),
                campaign.getScheduledTime(),
                campaign.getStatus()
        );
    }
}
