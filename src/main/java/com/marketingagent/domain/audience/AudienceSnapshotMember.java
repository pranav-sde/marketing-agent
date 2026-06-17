package com.marketingagent.domain.audience;

import com.marketingagent.domain.common.BaseEntity;
import com.marketingagent.domain.contact.Contact;
import com.marketingagent.domain.tenant.Tenant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(
        name = "audience_snapshot_members",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_audience_snapshot_members_snapshot_contact",
                columnNames = {"audience_snapshot_id", "contact_id"}
        ),
        indexes = {
                @Index(name = "idx_audience_snapshot_members_tenant", columnList = "tenant_id"),
                @Index(name = "idx_audience_snapshot_members_snapshot_eligible", columnList = "audience_snapshot_id,eligible")
        }
)
public class AudienceSnapshotMember extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "audience_snapshot_id", nullable = false)
    private AudienceSnapshot audienceSnapshot;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contact_id", nullable = false)
    private Contact contact;

    @Column(name = "eligible", nullable = false)
    private boolean eligible;

    @Column(name = "exclusion_reason", length = 160)
    private String exclusionReason;

    protected AudienceSnapshotMember() {
    }

    public AudienceSnapshotMember(Tenant tenant, AudienceSnapshot audienceSnapshot, Contact contact, boolean eligible) {
        this.tenant = tenant;
        this.audienceSnapshot = audienceSnapshot;
        this.contact = contact;
        this.eligible = eligible;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public AudienceSnapshot getAudienceSnapshot() {
        return audienceSnapshot;
    }

    public Contact getContact() {
        return contact;
    }

    public boolean isEligible() {
        return eligible;
    }

    public String getExclusionReason() {
        return exclusionReason;
    }

    public void setExclusionReason(String exclusionReason) {
        this.exclusionReason = exclusionReason;
    }
}
