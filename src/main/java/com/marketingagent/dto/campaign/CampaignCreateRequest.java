package com.marketingagent.dto.campaign;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CampaignCreateRequest(
        @NotBlank @Size(max = 180) String name,
        String objective,
        UUID segmentId,
        UUID ownerUserId
) {
}
