package com.marketingagent.repository;

import com.marketingagent.domain.campaign.CampaignAsset;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampaignAssetRepository extends JpaRepository<CampaignAsset, UUID> {
    List<CampaignAsset> findByTenant_IdAndCampaign_Id(UUID tenantId, UUID campaignId);
}
