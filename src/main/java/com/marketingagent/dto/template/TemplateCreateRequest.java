package com.marketingagent.dto.template;

import com.marketingagent.domain.template.TemplateCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;
import java.util.UUID;

public record TemplateCreateRequest(
        @NotNull UUID whatsAppAccountId,
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 16) String language,
        @NotNull TemplateCategory category,
        @NotNull Map<String, Object> components,
        Map<String, Object> variableExamples
) {
}
