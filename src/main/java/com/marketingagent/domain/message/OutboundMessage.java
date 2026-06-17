package com.marketingagent.domain.message;

import com.marketingagent.domain.broadcast.Broadcast;
import com.marketingagent.domain.broadcast.BroadcastBatch;
import com.marketingagent.domain.common.BaseEntity;
import com.marketingagent.domain.contact.Contact;
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
        name = "outbound_messages",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_outbound_messages_broadcast_contact",
                        columnNames = {"broadcast_id", "contact_id"}
                ),
                @UniqueConstraint(
                        name = "uk_outbound_messages_idempotency_key",
                        columnNames = "idempotency_key"
                )
        },
        indexes = {
                @Index(name = "idx_outbound_messages_tenant_status", columnList = "tenant_id,status"),
                @Index(name = "idx_outbound_messages_broadcast_status", columnList = "broadcast_id,status"),
                @Index(name = "idx_outbound_messages_provider_id", columnList = "provider_message_id")
        }
)
public class OutboundMessage extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "broadcast_id", nullable = false)
    private Broadcast broadcast;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "broadcast_batch_id")
    private BroadcastBatch broadcastBatch;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contact_id", nullable = false)
    private Contact contact;

    @NotBlank
    @Column(name = "idempotency_key", nullable = false, length = 160)
    private String idempotencyKey;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private OutboundMessageStatus status = OutboundMessageStatus.PENDING;

    @Column(name = "provider_message_id", length = 160)
    private String providerMessageId;

    @Column(name = "payload_hash", length = 128)
    private String payloadHash;

    @Column(name = "last_error_code", length = 120)
    private String lastErrorCode;

    @Column(name = "last_error_category", length = 120)
    private String lastErrorCategory;

    @Column(name = "sent_at")
    private Instant sentAt;

    protected OutboundMessage() {
    }

    public OutboundMessage(Tenant tenant, Broadcast broadcast, Contact contact, String idempotencyKey) {
        this.tenant = tenant;
        this.broadcast = broadcast;
        this.contact = contact;
        this.idempotencyKey = idempotencyKey;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public Broadcast getBroadcast() {
        return broadcast;
    }

    public BroadcastBatch getBroadcastBatch() {
        return broadcastBatch;
    }

    public void setBroadcastBatch(BroadcastBatch broadcastBatch) {
        this.broadcastBatch = broadcastBatch;
    }

    public Contact getContact() {
        return contact;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public OutboundMessageStatus getStatus() {
        return status;
    }

    public void setStatus(OutboundMessageStatus status) {
        this.status = status;
    }

    public String getProviderMessageId() {
        return providerMessageId;
    }

    public void setProviderMessageId(String providerMessageId) {
        this.providerMessageId = providerMessageId;
    }

    public String getPayloadHash() {
        return payloadHash;
    }

    public void setPayloadHash(String payloadHash) {
        this.payloadHash = payloadHash;
    }

    public String getLastErrorCode() {
        return lastErrorCode;
    }

    public void setLastErrorCode(String lastErrorCode) {
        this.lastErrorCode = lastErrorCode;
    }

    public String getLastErrorCategory() {
        return lastErrorCategory;
    }

    public void setLastErrorCategory(String lastErrorCategory) {
        this.lastErrorCategory = lastErrorCategory;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }
}
