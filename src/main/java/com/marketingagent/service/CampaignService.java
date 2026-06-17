package com.marketingagent.service;

import com.marketingagent.domain.audience.Segment;
import com.marketingagent.domain.campaign.ApprovalStatus;
import com.marketingagent.domain.campaign.Campaign;
import com.marketingagent.domain.campaign.CampaignApproval;
import com.marketingagent.domain.campaign.CampaignStatus;
import com.marketingagent.domain.tenant.Tenant;
import com.marketingagent.dto.campaign.CampaignCreateRequest;
import com.marketingagent.dto.campaign.CampaignDto;
import com.marketingagent.dto.campaign.CampaignReviewRequest;
import com.marketingagent.exception.BadRequestException;
import com.marketingagent.exception.ResourceNotFoundException;
import com.marketingagent.repository.CampaignApprovalRepository;
import com.marketingagent.repository.CampaignRepository;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class CampaignService {

    private final TenantService tenantService;
    private final AudienceService audienceService;
    private final CampaignRepository campaignRepository;
    private final CampaignApprovalRepository campaignApprovalRepository;

    public CampaignService(
            TenantService tenantService,
            AudienceService audienceService,
            CampaignRepository campaignRepository,
            CampaignApprovalRepository campaignApprovalRepository
    ) {
        this.tenantService = tenantService;
        this.audienceService = audienceService;
        this.campaignRepository = campaignRepository;
        this.campaignApprovalRepository = campaignApprovalRepository;
    }

    @Transactional
    public CampaignDto createCampaign(UUID tenantId, @Valid CampaignCreateRequest request) {
        Tenant tenant = tenantService.getTenantEntity(tenantId);
        Campaign campaign = new Campaign(tenant, request.name());
        campaign.setObjective(request.objective());
        campaign.setOwnerUserId(request.ownerUserId());
        if (request.segmentId() != null) {
            Segment segment = audienceService.getSegmentEntity(tenantId, request.segmentId());
            campaign.setSegment(segment);
        }
        return CampaignDto.from(campaignRepository.save(campaign));
    }

    @Transactional(readOnly = true)
    public Campaign getCampaignEntity(UUID tenantId, UUID campaignId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign", campaignId));
        if (!campaign.getTenant().getId().equals(tenantId)) {
            throw new ResourceNotFoundException("Campaign", campaignId);
        }
        return campaign;
    }

    @Transactional
    public CampaignDto submitForReview(UUID tenantId, UUID campaignId) {
        Campaign campaign = getCampaignEntity(tenantId, campaignId);
        if (campaign.getStatus() != CampaignStatus.DRAFT && campaign.getStatus() != CampaignStatus.REJECTED) {
            throw new BadRequestException("Only draft or rejected campaigns can be submitted for review");
        }
        campaign.setStatus(CampaignStatus.IN_REVIEW);
        campaignApprovalRepository.save(new CampaignApproval(campaign.getTenant(), campaign));
        return CampaignDto.from(campaignRepository.save(campaign));
    }

    @Transactional
    public CampaignDto approveCampaign(UUID tenantId, UUID campaignId, @Valid CampaignReviewRequest request) {
        Campaign campaign = getCampaignEntity(tenantId, campaignId);
        CampaignApproval approval = campaignApprovalRepository.findFirstByCampaign_IdOrderByCreatedAtDesc(campaignId)
                .orElseGet(() -> new CampaignApproval(campaign.getTenant(), campaign));
        approval.setStatus(ApprovalStatus.APPROVED);
        approval.setReviewerUserId(request.reviewerUserId());
        approval.setReviewedAt(Instant.now());
        approval.setReason(request.reason());
        campaignApprovalRepository.save(approval);
        campaign.setStatus(CampaignStatus.APPROVED);
        return CampaignDto.from(campaignRepository.save(campaign));
    }

    @Transactional
    public CampaignDto rejectCampaign(UUID tenantId, UUID campaignId, @Valid CampaignReviewRequest request) {
        Campaign campaign = getCampaignEntity(tenantId, campaignId);
        CampaignApproval approval = campaignApprovalRepository.findFirstByCampaign_IdOrderByCreatedAtDesc(campaignId)
                .orElseGet(() -> new CampaignApproval(campaign.getTenant(), campaign));
        approval.setStatus(ApprovalStatus.REJECTED);
        approval.setReviewerUserId(request.reviewerUserId());
        approval.setReviewedAt(Instant.now());
        approval.setReason(request.reason());
        campaignApprovalRepository.save(approval);
        campaign.setStatus(CampaignStatus.REJECTED);
        return CampaignDto.from(campaignRepository.save(campaign));
    }

    @Transactional(readOnly = true)
    public List<CampaignDto> listCampaigns(UUID tenantId) {
        return campaignRepository.findAll().stream()
                .filter(campaign -> campaign.getTenant().getId().equals(tenantId))
                .map(CampaignDto::from)
                .toList();
    }
}
