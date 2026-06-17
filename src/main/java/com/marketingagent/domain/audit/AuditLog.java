package com.marketingagent.domain.audit;

import com.marketingagent.domain.common.BaseEntity;
import com.marketingagent.domain.tenant.Tenant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(
        name = "audit_log",
        indexes = {
                @Index(name = "idx_audit_log_tenant_action", columnList = "tenant_id,action_type"),
                @Index(name = "idx_audit_log_occurred", columnList = "occurred_at"),
                @Index(name = "idx_audit_log_actor", columnList = "actor_user_id")
        }
)
public class AuditLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 64)
    private AuditActionType actionType;

    @Column(name = "entity_type", length = 120)
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @NotNull
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "details", columnDefinition = "jsonb")
    private Map<String, Object> details = new LinkedHashMap<>();

    protected AuditLog() {
    }

    public AuditLog(Tenant tenant, UUID actorUserId, AuditActionType actionType, Instant occurredAt) {
        this.tenant = tenant;
        this.actorUserId = actorUserId;
        this.actionType = actionType;
        this.occurredAt = occurredAt;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public AuditActionType getActionType() {
        return actionType;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public void setEntityId(UUID entityId) {
        this.entityId = entityId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = new LinkedHashMap<>(details);
    }
}
