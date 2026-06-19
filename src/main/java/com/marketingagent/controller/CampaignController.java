package com.marketingagent.controller;

import com.marketingagent.dto.campaign.CampaignCreateRequest;
import com.marketingagent.dto.campaign.CampaignDto;
import com.marketingagent.dto.campaign.CampaignReviewRequest;
import com.marketingagent.service.CampaignService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/tenants/{tenantId}/campaigns")
public class CampaignController {

    private final CampaignService campaignService;

    public CampaignController(CampaignService campaignService) {
        this.campaignService = campaignService;
    }

    @PostMapping
    public ResponseEntity<CampaignDto> createCampaign(
            @PathVariable UUID tenantId,
            @Valid @RequestBody CampaignCreateRequest request
    ) {
        CampaignDto created = campaignService.createCampaign(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<CampaignDto>> listCampaigns(@PathVariable UUID tenantId) {
        List<CampaignDto> campaigns = campaignService.listCampaigns(tenantId);
        return ResponseEntity.ok(campaigns);
    }

    @GetMapping("/{campaignId}")
    public ResponseEntity<CampaignDto> getCampaign(
            @PathVariable UUID tenantId,
            @PathVariable UUID campaignId
    ) {
        CampaignDto campaign = CampaignDto.from(campaignService.getCampaignEntity(tenantId, campaignId));
        return ResponseEntity.ok(campaign);
    }

    @PostMapping("/{campaignId}/submit-for-review")
    public ResponseEntity<CampaignDto> submitForReview(
            @PathVariable UUID tenantId,
            @PathVariable UUID campaignId
    ) {
        CampaignDto updated = campaignService.submitForReview(tenantId, campaignId);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{campaignId}/approve")
    public ResponseEntity<CampaignDto> approveCampaign(
            @PathVariable UUID tenantId,
            @PathVariable UUID campaignId,
            @Valid @RequestBody CampaignReviewRequest request
    ) {
        CampaignDto approved = campaignService.approveCampaign(tenantId, campaignId, request);
        return ResponseEntity.ok(approved);
    }

    @PostMapping("/{campaignId}/reject")
    public ResponseEntity<CampaignDto> rejectCampaign(
            @PathVariable UUID tenantId,
            @PathVariable UUID campaignId,
            @Valid @RequestBody CampaignReviewRequest request
    ) {
        CampaignDto rejected = campaignService.rejectCampaign(tenantId, campaignId, request);
        return ResponseEntity.ok(rejected);
    }
}
