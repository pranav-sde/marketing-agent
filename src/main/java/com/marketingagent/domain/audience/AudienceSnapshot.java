package com.marketingagent.domain.audience;

import com.marketingagent.domain.common.BaseEntity;
import com.marketingagent.domain.tenant.Tenant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

@Entity
@Table(
        name = "audience_snapshots",
        indexes = {
                @Index(name = "idx_audience_snapshots_tenant", columnList = "tenant_id"),
                @Index(name = "idx_audience_snapshots_segment", columnList = "segment_id")
        }
)
public class AudienceSnapshot extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "segment_id")
    private Segment segment;

    @NotNull
    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;

    @Column(name = "total_count", nullable = false)
    private long totalCount;

    @Column(name = "eligible_count", nullable = false)
    private long eligibleCount;

    @Column(name = "excluded_count", nullable = false)
    private long excludedCount;

    protected AudienceSnapshot() {
    }

    public AudienceSnapshot(Tenant tenant, Segment segment, Instant capturedAt) {
        this.tenant = tenant;
        this.segment = segment;
        this.capturedAt = capturedAt;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public Segment getSegment() {
        return segment;
    }

    public Instant getCapturedAt() {
        return capturedAt;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }

    public long getEligibleCount() {
        return eligibleCount;
    }

    public void setEligibleCount(long eligibleCount) {
        this.eligibleCount = eligibleCount;
    }

    public long getExcludedCount() {
        return excludedCount;
    }

    public void setExcludedCount(long excludedCount) {
        this.excludedCount = excludedCount;
    }
}
