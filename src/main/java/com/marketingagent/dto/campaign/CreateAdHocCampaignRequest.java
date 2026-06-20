package com.marketingagent.dto.campaign;

import com.marketingagent.domain.magazine.ContentPlatform;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record CreateAdHocCampaignRequest(
        String messageText,
        String mediaUrl,
        @NotNull ContentPlatform platform,
        @NotNull Instant scheduledTime
) {
}
