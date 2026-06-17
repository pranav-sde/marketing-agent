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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

@Entity
@Table(
        name = "consent_events",
        indexes = {
                @Index(name = "idx_consent_events_tenant_contact", columnList = "tenant_id,contact_id"),
                @Index(name = "idx_consent_events_channel_time", columnList = "channel,captured_at")
        }
)
public class ConsentEvent extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contact_id", nullable = false)
    private Contact contact;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 32)
    private Channel channel;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 32)
    private ConsentEventType eventType;

    @NotBlank
    @Column(name = "source", nullable = false, length = 160)
    private String source;

    @NotNull
    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;

    @Column(name = "policy_version", length = 80)
    private String policyVersion;

    @Column(name = "evidence_ref", length = 512)
    private String evidenceRef;

    protected ConsentEvent() {
    }

    public ConsentEvent(
            Tenant tenant,
            Contact contact,
            Channel channel,
            ConsentEventType eventType,
            String source,
            Instant capturedAt
    ) {
        this.tenant = tenant;
        this.contact = contact;
        this.channel = channel;
        this.eventType = eventType;
        this.source = source;
        this.capturedAt = capturedAt;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public Contact getContact() {
        return contact;
    }

    public Channel getChannel() {
        return channel;
    }

    public ConsentEventType getEventType() {
        return eventType;
    }

    public String getSource() {
        return source;
    }

    public Instant getCapturedAt() {
        return capturedAt;
    }

    public String getPolicyVersion() {
        return policyVersion;
    }

    public void setPolicyVersion(String policyVersion) {
        this.policyVersion = policyVersion;
    }

    public String getEvidenceRef() {
        return evidenceRef;
    }

    public void setEvidenceRef(String evidenceRef) {
        this.evidenceRef = evidenceRef;
    }
}
