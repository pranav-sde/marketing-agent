package com.marketingagent.webclient.whatsapp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

public record WhatsAppTemplateMessageRequest(
        @NotBlank String to,
        @NotBlank String templateName,
        @NotBlank String languageCode,
        List<Map<String, Object>> components,
        @NotNull Map<String, Object> metadata
) {
}
