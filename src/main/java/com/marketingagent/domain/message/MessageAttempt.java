package com.marketingagent.domain.message;

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
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

@Entity
@Table(
        name = "message_attempts",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_message_attempts_idempotency_key",
                columnNames = "idempotency_key"
        ),
        indexes = {
                @Index(name = "idx_message_attempts_tenant_status", columnList = "tenant_id,status"),
                @Index(name = "idx_message_attempts_message", columnList = "outbound_message_id")
        }
)
public class MessageAttempt extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "outbound_message_id", nullable = false)
    private OutboundMessage outboundMessage;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @NotBlank
    @Column(name = "idempotency_key", nullable = false, length = 180)
    private String idempotencyKey;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private MessageAttemptStatus status = MessageAttemptStatus.PENDING;

    @Column(name = "provider_status_code")
    private Integer providerStatusCode;

    @Column(name = "provider_error_code", length = 120)
    private String providerErrorCode;

    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt;

    protected MessageAttempt() {
    }

    public MessageAttempt(
            Tenant tenant,
            OutboundMessage outboundMessage,
            int attemptNumber,
            String idempotencyKey,
            Instant attemptedAt
    ) {
        this.tenant = tenant;
        this.outboundMessage = outboundMessage;
        this.attemptNumber = attemptNumber;
        this.idempotencyKey = idempotencyKey;
        this.attemptedAt = attemptedAt;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public OutboundMessage getOutboundMessage() {
        return outboundMessage;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public MessageAttemptStatus getStatus() {
        return status;
    }

    public void setStatus(MessageAttemptStatus status) {
        this.status = status;
    }

    public Integer getProviderStatusCode() {
        return providerStatusCode;
    }

    public void setProviderStatusCode(Integer providerStatusCode) {
        this.providerStatusCode = providerStatusCode;
    }

    public String getProviderErrorCode() {
        return providerErrorCode;
    }

    public void setProviderErrorCode(String providerErrorCode) {
        this.providerErrorCode = providerErrorCode;
    }

    public Instant getAttemptedAt() {
        return attemptedAt;
    }
}
