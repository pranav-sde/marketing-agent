package com.marketingagent.repository;

import com.marketingagent.domain.campaign.AdHocCampaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface AdHocCampaignRepository extends JpaRepository<AdHocCampaign, UUID> {
    List<AdHocCampaign> findByTenant_IdOrderByScheduledTimeDesc(UUID tenantId);
}
