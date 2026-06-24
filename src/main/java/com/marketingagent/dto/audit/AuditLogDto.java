package com.marketingagent.dto.audit;

import com.marketingagent.domain.audit.AuditActionType;
import com.marketingagent.domain.audit.AuditLog;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditLogDto(
        UUID id,
        UUID tenantId,
        AuditActionType actionType,
        String entityType,
        UUID entityId,
        Instant occurredAt,
        Map<String, Object> details
) {
    public static AuditLogDto from(AuditLog log) {
        return new AuditLogDto(
                log.getId(),
                log.getTenant() != null ? log.getTenant().getId() : null,
                log.getActionType(),
                log.getEntityType(),
                log.getEntityId(),
                log.getOccurredAt(),
                log.getDetails()
        );
    }
}
