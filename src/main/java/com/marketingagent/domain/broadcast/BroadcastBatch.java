package com.marketingagent.domain.broadcast;

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
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "broadcast_batches",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_broadcast_batches_broadcast_sequence",
                columnNames = {"broadcast_id", "sequence_number"}
        ),
        indexes = {
                @Index(name = "idx_broadcast_batches_tenant_status", columnList = "tenant_id,status"),
                @Index(name = "idx_broadcast_batches_lease", columnList = "lease_expires_at")
        }
)
public class BroadcastBatch extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "broadcast_id", nullable = false)
    private Broadcast broadcast;

    @Column(name = "sequence_number", nullable = false)
    private int sequenceNumber;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private BroadcastBatchStatus status = BroadcastBatchStatus.PENDING;

    @Column(name = "lease_owner")
    private UUID leaseOwner;

    @Column(name = "lease_expires_at")
    private Instant leaseExpiresAt;

    @Column(name = "message_count", nullable = false)
    private int messageCount;

    protected BroadcastBatch() {
    }

    public BroadcastBatch(Tenant tenant, Broadcast broadcast, int sequenceNumber, int messageCount) {
        this.tenant = tenant;
        this.broadcast = broadcast;
        this.sequenceNumber = sequenceNumber;
        this.messageCount = messageCount;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public Broadcast getBroadcast() {
        return broadcast;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public BroadcastBatchStatus getStatus() {
        return status;
    }

    public void setStatus(BroadcastBatchStatus status) {
        this.status = status;
    }

    public UUID getLeaseOwner() {
        return leaseOwner;
    }

    public void setLeaseOwner(UUID leaseOwner) {
        this.leaseOwner = leaseOwner;
    }

    public Instant getLeaseExpiresAt() {
        return leaseExpiresAt;
    }

    public void setLeaseExpiresAt(Instant leaseExpiresAt) {
        this.leaseExpiresAt = leaseExpiresAt;
    }

    public int getMessageCount() {
        return messageCount;
    }
}
