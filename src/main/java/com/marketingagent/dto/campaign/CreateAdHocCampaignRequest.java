package com.marketingagent.dto.campaign;

import com.marketingagent.domain.magazine.ContentPlatform;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record CreateAdHocCampaignRequest(
        String messageText,
        String mediaUrl,
        @NotNull ContentPlatform platform,
        @NotNull Instant scheduledTime
) {
    @AssertTrue(message = "Either messageText or mediaUrl must be provided")
    public boolean hasContent() {
        return (messageText != null && !messageText.trim().isEmpty())
                || (mediaUrl != null && !mediaUrl.trim().isEmpty());
    }
}
