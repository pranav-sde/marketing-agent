package com.marketingagent.dto.message;

import com.marketingagent.domain.common.ProviderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.UUID;

public record WebhookIngestRequest(
        UUID whatsAppAccountId,
        @NotNull ProviderType provider,
        String providerEventId,
        @NotBlank String eventHash,
        @NotNull Map<String, Object> payload
) {
}
