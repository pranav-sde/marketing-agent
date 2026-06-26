package com.marketingagent.repository;

import com.marketingagent.model.AdHocCampaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface AdHocCampaignRepository extends JpaRepository<AdHocCampaign, UUID> {
    List<AdHocCampaign> findByTenantIdOrderByScheduledTimeDesc(UUID tenantId);
    List<AdHocCampaign> findByStatusAndScheduledTimeLessThanEqual(String status, Instant time);
}
