package com.marketingagent.repository;

import com.marketingagent.domain.campaign.ApprovalStatus;
import com.marketingagent.domain.campaign.CampaignApproval;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampaignApprovalRepository extends JpaRepository<CampaignApproval, UUID> {
    List<CampaignApproval> findByTenant_IdAndStatus(UUID tenantId, ApprovalStatus status);

    Optional<CampaignApproval> findFirstByCampaign_IdOrderByCreatedAtDesc(UUID campaignId);
}
