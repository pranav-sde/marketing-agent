package com.marketingagent.dto.tenant;

import com.marketingagent.domain.tenant.Tenant;
import com.marketingagent.domain.tenant.TenantStatus;
import java.time.Instant;
import java.util.UUID;

public record TenantDto(
        UUID id,
        String name,
        String slug,
        TenantStatus status,
        String timezone,
        String defaultLocale,
        Instant createdAt,
        Instant updatedAt
) {
    public static TenantDto from(Tenant tenant) {
        return new TenantDto(
                tenant.getId(),
                tenant.getName(),
                tenant.getSlug(),
                tenant.getStatus(),
                tenant.getTimezone(),
                tenant.getDefaultLocale(),
                tenant.getCreatedAt(),
                tenant.getUpdatedAt()
        );
    }
}
