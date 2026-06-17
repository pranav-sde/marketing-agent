package com.marketingagent.dto.campaign;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CampaignReviewRequest(
        @NotNull UUID reviewerUserId,
        String reason
) {
}
