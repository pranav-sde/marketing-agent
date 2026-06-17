package com.marketingagent.repository;

import com.marketingagent.domain.campaign.Campaign;
import com.marketingagent.domain.campaign.CampaignStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampaignRepository extends JpaRepository<Campaign, UUID> {
    List<Campaign> findByTenant_IdAndStatus(UUID tenantId, CampaignStatus status);

    List<Campaign> findByTenant_IdAndOwnerUserId(UUID tenantId, UUID ownerUserId);
}
