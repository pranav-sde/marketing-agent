package com.marketingagent.domain.contact;

import com.marketingagent.domain.common.BaseEntity;
import com.marketingagent.domain.common.Channel;
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
        name = "suppression_list",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_suppression_tenant_channel_phone_hash",
                columnNames = {"tenant_id", "channel", "phone_hash"}
        ),
        indexes = @Index(name = "idx_suppression_tenant_reason", columnList = "tenant_id,reason")
)
public class SuppressionListEntry extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 32)
    private Channel channel;

    @NotBlank
    @Column(name = "phone_hash", nullable = false, length = 128)
    private String phoneHash;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 48)
    private SuppressionReason reason;

    @NotNull
    @Column(name = "suppressed_at", nullable = false)
    private Instant suppressedAt;

    @Column(name = "source", length = 160)
    private String source;

    protected SuppressionListEntry() {
    }

    public SuppressionListEntry(
            Tenant tenant,
            Channel channel,
            String phoneHash,
            SuppressionReason reason,
            Instant suppressedAt
    ) {
        this.tenant = tenant;
        this.channel = channel;
        this.phoneHash = phoneHash;
        this.reason = reason;
        this.suppressedAt = suppressedAt;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public Channel getChannel() {
        return channel;
    }

    public String getPhoneHash() {
        return phoneHash;
    }

    public SuppressionReason getReason() {
        return reason;
    }

    public Instant getSuppressedAt() {
        return suppressedAt;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
