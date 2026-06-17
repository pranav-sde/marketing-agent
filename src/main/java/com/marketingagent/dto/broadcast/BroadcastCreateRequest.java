package com.marketingagent.dto.broadcast;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public record BroadcastCreateRequest(
        @NotNull UUID campaignId,
        @NotNull UUID audienceSnapshotId,
        @NotNull UUID messageTemplateId,
        @NotNull UUID templateVersionId,
        @NotNull UUID whatsAppAccountId,
        Instant scheduledStartAt,
        Instant scheduledEndAt,
        @Size(max = 64) String timezone,
        @Size(max = 80) String rateLimitProfile
) {
}
