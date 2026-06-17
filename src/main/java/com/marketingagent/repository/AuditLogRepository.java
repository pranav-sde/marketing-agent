package com.marketingagent.repository;

import com.marketingagent.domain.audit.AuditActionType;
import com.marketingagent.domain.audit.AuditLog;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    List<AuditLog> findByTenant_IdAndActionType(UUID tenantId, AuditActionType actionType);

    List<AuditLog> findByTenant_IdAndOccurredAtBetween(UUID tenantId, Instant start, Instant end);
}
