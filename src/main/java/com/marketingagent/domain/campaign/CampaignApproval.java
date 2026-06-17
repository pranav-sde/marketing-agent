package com.marketingagent.domain.campaign;

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
import java.time.Instant;

@Entity
@Table(
        name = "campaign_approvals",
        indexes = {
                @Index(name = "idx_campaign_approvals_tenant", columnList = "tenant_id"),
                @Index(name = "idx_campaign_approvals_campaign", columnList = "campaign_id")
        }
)
public class CampaignApproval extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ApprovalStatus status = ApprovalStatus.PENDING;

    @Column(name = "reviewer_user_id")
    private java.util.UUID reviewerUserId;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "reason", columnDefinition = "text")
    private String reason;

    protected CampaignApproval() {
    }

    public CampaignApproval(Tenant tenant, Campaign campaign) {
        this.tenant = tenant;
        this.campaign = campaign;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public ApprovalStatus getStatus() {
        return status;
    }

    public void setStatus(ApprovalStatus status) {
        this.status = status;
    }

    public java.util.UUID getReviewerUserId() {
        return reviewerUserId;
    }

    public void setReviewerUserId(java.util.UUID reviewerUserId) {
        this.reviewerUserId = reviewerUserId;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Instant reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
