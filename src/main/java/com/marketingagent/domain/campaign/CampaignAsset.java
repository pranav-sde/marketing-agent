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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(
        name = "campaign_assets",
        indexes = {
                @Index(name = "idx_campaign_assets_tenant", columnList = "tenant_id"),
                @Index(name = "idx_campaign_assets_campaign", columnList = "campaign_id")
        }
)
public class CampaignAsset extends BaseEntity {

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
    @Column(name = "type", nullable = false, length = 32)
    private CampaignAssetType type;

    @NotBlank
    @Column(name = "storage_ref", nullable = false, length = 512)
    private String storageRef;

    @Column(name = "content_type", length = 120)
    private String contentType;

    protected CampaignAsset() {
    }

    public CampaignAsset(Tenant tenant, Campaign campaign, CampaignAssetType type, String storageRef) {
        this.tenant = tenant;
        this.campaign = campaign;
        this.type = type;
        this.storageRef = storageRef;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public CampaignAssetType getType() {
        return type;
    }

    public String getStorageRef() {
        return storageRef;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
}
