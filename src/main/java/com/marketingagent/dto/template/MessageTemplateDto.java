package com.marketingagent.dto.template;

import com.marketingagent.domain.template.MessageTemplate;
import com.marketingagent.domain.template.TemplateCategory;
import com.marketingagent.domain.template.TemplateStatus;
import java.time.Instant;
import java.util.UUID;

public record MessageTemplateDto(
        UUID id,
        UUID tenantId,
        UUID whatsAppAccountId,
        String name,
        String language,
        TemplateCategory category,
        TemplateStatus status,
        String providerTemplateId,
        Instant lastSyncedAt
) {
    public static MessageTemplateDto from(MessageTemplate template) {
        return new MessageTemplateDto(
                template.getId(),
                template.getTenant().getId(),
                template.getWhatsAppAccount().getId(),
                template.getName(),
                template.getLanguage(),
                template.getCategory(),
                template.getStatus(),
                template.getProviderTemplateId(),
                template.getLastSyncedAt()
        );
    }
}
