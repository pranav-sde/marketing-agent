package com.marketingagent.domain.campaign;

import com.marketingagent.domain.audience.Segment;
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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(
        name = "campaigns",
        indexes = {
                @Index(name = "idx_campaigns_tenant_status", columnList = "tenant_id,status"),
                @Index(name = "idx_campaigns_segment", columnList = "segment_id")
        }
)
public class Campaign extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "segment_id")
    private Segment segment;

    @NotBlank
    @Column(name = "name", nullable = false, length = 180)
    private String name;

    @Column(name = "objective", columnDefinition = "text")
    private String objective;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private CampaignStatus status = CampaignStatus.DRAFT;

    @Column(name = "owner_user_id")
    private java.util.UUID ownerUserId;

    protected Campaign() {
    }

    public Campaign(Tenant tenant, String name) {
        this.tenant = tenant;
        this.name = name;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public Segment getSegment() {
        return segment;
    }

    public void setSegment(Segment segment) {
        this.segment = segment;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    public CampaignStatus getStatus() {
        return status;
    }

    public void setStatus(CampaignStatus status) {
        this.status = status;
    }

    public java.util.UUID getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(java.util.UUID ownerUserId) {
        this.ownerUserId = ownerUserId;
    }
}
