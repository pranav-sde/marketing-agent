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
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

@Entity
@Table(
        name = "contact_channel_state",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_contact_channel_state_contact_channel",
                columnNames = {"contact_id", "channel"}
        ),
        indexes = {
                @Index(name = "idx_contact_channel_state_tenant_status", columnList = "tenant_id,status"),
                @Index(name = "idx_contact_channel_state_contact", columnList = "contact_id")
        }
)
public class ContactChannelState extends BaseEntity {

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
    @Column(name = "status", nullable = false, length = 32)
    private ContactStatus status = ContactStatus.ACTIVE;

    @Column(name = "last_consent_event_at")
    private Instant lastConsentEventAt;

    protected ContactChannelState() {
    }

    public ContactChannelState(Tenant tenant, Contact contact, Channel channel) {
        this.tenant = tenant;
        this.contact = contact;
        this.channel = channel;
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

    public ContactStatus getStatus() {
        return status;
    }

    public void setStatus(ContactStatus status) {
        this.status = status;
    }

    public Instant getLastConsentEventAt() {
        return lastConsentEventAt;
    }

    public void setLastConsentEventAt(Instant lastConsentEventAt) {
        this.lastConsentEventAt = lastConsentEventAt;
    }
}
