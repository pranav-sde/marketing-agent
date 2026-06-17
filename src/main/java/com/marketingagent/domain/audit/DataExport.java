package com.marketingagent.domain.audit;

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
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "data_exports",
        indexes = {
                @Index(name = "idx_data_exports_tenant_status", columnList = "tenant_id,status"),
                @Index(name = "idx_data_exports_requested", columnList = "requested_at")
        }
)
public class DataExport extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "requested_by_user_id")
    private UUID requestedByUserId;

    @NotBlank
    @Column(name = "export_type", nullable = false, length = 120)
    private String exportType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private DataExportStatus status = DataExportStatus.REQUESTED;

    @NotNull
    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "storage_ref", length = 512)
    private String storageRef;

    protected DataExport() {
    }

    public DataExport(Tenant tenant, String exportType, Instant requestedAt) {
        this.tenant = tenant;
        this.exportType = exportType;
        this.requestedAt = requestedAt;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public UUID getRequestedByUserId() {
        return requestedByUserId;
    }

    public void setRequestedByUserId(UUID requestedByUserId) {
        this.requestedByUserId = requestedByUserId;
    }

    public String getExportType() {
        return exportType;
    }

    public DataExportStatus getStatus() {
        return status;
    }

    public void setStatus(DataExportStatus status) {
        this.status = status;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public String getStorageRef() {
        return storageRef;
    }

    public void setStorageRef(String storageRef) {
        this.storageRef = storageRef;
    }
}
