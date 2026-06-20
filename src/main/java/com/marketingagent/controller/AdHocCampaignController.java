package com.marketingagent.controller;

import com.marketingagent.dto.campaign.AdHocCampaignDto;
import com.marketingagent.dto.campaign.CreateAdHocCampaignRequest;
import com.marketingagent.dto.message.ContentAnalyticsDto;
import com.marketingagent.service.AdHocCampaignService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/tenants/{tenantId}/adhoc-campaigns")
public class AdHocCampaignController {

    private final AdHocCampaignService adHocCampaignService;

    public AdHocCampaignController(AdHocCampaignService adHocCampaignService) {
        this.adHocCampaignService = adHocCampaignService;
    }

    @PostMapping
    public ResponseEntity<AdHocCampaignDto> createCampaign(
            @PathVariable UUID tenantId,
            @RequestBody CreateAdHocCampaignRequest request) {
        return ResponseEntity.ok(adHocCampaignService.createCampaign(tenantId, request));
    }

    @GetMapping
    public ResponseEntity<List<AdHocCampaignDto>> getCampaigns(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(adHocCampaignService.getCampaigns(tenantId));
    }

    @PostMapping(value = "/{campaignId}/media", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AdHocCampaignDto> uploadCampaignMedia(
            @PathVariable UUID tenantId,
            @PathVariable UUID campaignId,
            @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(adHocCampaignService.uploadCampaignMedia(tenantId, campaignId, file));
    }

    @GetMapping("/{campaignId}/analytics")
    public ResponseEntity<ContentAnalyticsDto> getAnalytics(
            @PathVariable UUID tenantId,
            @PathVariable UUID campaignId) {
        return ResponseEntity.ok(adHocCampaignService.getAnalytics(tenantId, campaignId));
    }

    @PostMapping("/{campaignId}/pause")
    public ResponseEntity<AdHocCampaignDto> pauseCampaign(
            @PathVariable UUID tenantId,
            @PathVariable UUID campaignId) {
        return ResponseEntity.ok(adHocCampaignService.pauseCampaign(tenantId, campaignId));
    }

    @PostMapping("/{campaignId}/resume")
    public ResponseEntity<AdHocCampaignDto> resumeCampaign(
            @PathVariable UUID tenantId,
            @PathVariable UUID campaignId) {
        return ResponseEntity.ok(adHocCampaignService.resumeCampaign(tenantId, campaignId));
    }

    @PostMapping("/{campaignId}/cancel")
    public ResponseEntity<AdHocCampaignDto> cancelCampaign(
            @PathVariable UUID tenantId,
            @PathVariable UUID campaignId) {
        return ResponseEntity.ok(adHocCampaignService.cancelCampaign(tenantId, campaignId));
    }

    @PostMapping("/{campaignId}/reschedule")
    public ResponseEntity<AdHocCampaignDto> rescheduleCampaign(
            @PathVariable UUID tenantId,
            @PathVariable UUID campaignId,
            @RequestBody java.util.Map<String, String> request) {
        if (!request.containsKey("scheduledTime")) {
            return ResponseEntity.badRequest().build();
        }
        java.time.Instant newTime = java.time.Instant.parse(request.get("scheduledTime"));
        return ResponseEntity.ok(adHocCampaignService.rescheduleCampaign(tenantId, campaignId, newTime));
    }
}
