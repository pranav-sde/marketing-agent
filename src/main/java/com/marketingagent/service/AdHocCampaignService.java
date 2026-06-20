package com.marketingagent.service;

import com.marketingagent.domain.campaign.AdHocCampaign;
import com.marketingagent.domain.tenant.Tenant;
import com.marketingagent.dto.campaign.AdHocCampaignDto;
import com.marketingagent.dto.campaign.CreateAdHocCampaignRequest;
import com.marketingagent.exception.ResourceNotFoundException;
import com.marketingagent.quartz.AdHocBroadcastJob;
import com.marketingagent.repository.AdHocCampaignRepository;
import com.marketingagent.repository.TenantRepository;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AdHocCampaignService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdHocCampaignService.class);

    private final AdHocCampaignRepository adHocCampaignRepository;
    private final TenantRepository tenantRepository;
    private final Scheduler scheduler;
    private final StorageService storageService;
    private final com.marketingagent.repository.ContentOutboundMessageRepository contentOutboundMessageRepository;

    public AdHocCampaignService(
            AdHocCampaignRepository adHocCampaignRepository,
            TenantRepository tenantRepository,
            Scheduler scheduler,
            StorageService storageService,
            com.marketingagent.repository.ContentOutboundMessageRepository contentOutboundMessageRepository) {
        this.adHocCampaignRepository = adHocCampaignRepository;
        this.tenantRepository = tenantRepository;
        this.scheduler = scheduler;
        this.storageService = storageService;
        this.contentOutboundMessageRepository = contentOutboundMessageRepository;
    }

    @Transactional
    public AdHocCampaignDto createCampaign(UUID tenantId, CreateAdHocCampaignRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));

        Instant scheduledTime = request.scheduledTime();
        if (scheduledTime.isBefore(Instant.now())) {
            scheduledTime = Instant.now().plusSeconds(60); // Schedule at least 1 min in future if past
        }

        AdHocCampaign campaign = new AdHocCampaign(
                tenant,
                request.messageText(),
                request.mediaUrl(),
                request.platform(),
                scheduledTime
        );

        campaign = adHocCampaignRepository.save(campaign);

        scheduleJob(campaign);

        return AdHocCampaignDto.from(campaign);
    }

    @Transactional(readOnly = true)
    public List<AdHocCampaignDto> getCampaigns(UUID tenantId) {
        return adHocCampaignRepository.findByTenant_IdOrderByScheduledTimeDesc(tenantId)
                .stream()
                .map(AdHocCampaignDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public AdHocCampaignDto uploadCampaignMedia(UUID tenantId, UUID campaignId, org.springframework.web.multipart.MultipartFile file) {
        AdHocCampaign campaign = adHocCampaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("AdHocCampaign", campaignId));
        if (!campaign.getTenant().getId().equals(tenantId)) {
            throw new ResourceNotFoundException("AdHocCampaign", campaignId);
        }

        try {
            String extension = "jpg";
            if (file.getOriginalFilename() != null && file.getOriginalFilename().toLowerCase().endsWith(".png")) {
                extension = "png";
            }
            String key = "adhoc-media-" + UUID.randomUUID() + "." + extension;
            String mediaUrl = storageService.uploadFile(key, file.getBytes(), file.getContentType());
            campaign.setMediaUrl(mediaUrl);
            return AdHocCampaignDto.from(adHocCampaignRepository.save(campaign));
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to read file", e);
        }
    }

    @Transactional(readOnly = true)
    public com.marketingagent.dto.message.ContentAnalyticsDto getAnalytics(UUID tenantId, UUID campaignId) {
        AdHocCampaign campaign = adHocCampaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("AdHocCampaign", campaignId));
        if (!campaign.getTenant().getId().equals(tenantId)) {
            throw new ResourceNotFoundException("AdHocCampaign", campaignId);
        }

        List<Object[]> counts = contentOutboundMessageRepository.countStatusByAdHocCampaignId(campaignId);
        long sent = 0;
        long delivered = 0;
        long read = 0;
        long failed = 0;

        for (Object[] row : counts) {
            com.marketingagent.domain.message.OutboundMessageStatus status = (com.marketingagent.domain.message.OutboundMessageStatus) row[0];
            long count = ((Number) row[1]).longValue();
            switch (status) {
                case SENT: sent += count; break;
                case DELIVERED: delivered += count; break;
                case READ: read += count; break;
                case FAILED: failed += count; break;
                default: break;
            }
        }
        return new com.marketingagent.dto.message.ContentAnalyticsDto(sent, delivered, read, failed);
    }

    @Transactional
    public AdHocCampaignDto pauseCampaign(UUID tenantId, UUID campaignId) {
        AdHocCampaign campaign = getAndVerifyCampaign(tenantId, campaignId);
        if (campaign.getStatus() == com.marketingagent.domain.campaign.AdHocCampaignStatus.SCHEDULED) {
            campaign.setStatus(com.marketingagent.domain.campaign.AdHocCampaignStatus.PAUSED);
            try {
                org.quartz.JobKey jobKey = org.quartz.JobKey.jobKey("adhoc-campaign-" + campaign.getId(), "adhoc-campaigns");
                scheduler.interrupt(jobKey);
                scheduler.deleteJob(jobKey); // Remove future trigger
            } catch (SchedulerException e) {
                LOGGER.error("Failed to pause job in scheduler", e);
            }
        }
        return AdHocCampaignDto.from(adHocCampaignRepository.save(campaign));
    }

    @Transactional
    public AdHocCampaignDto resumeCampaign(UUID tenantId, UUID campaignId) {
        AdHocCampaign campaign = getAndVerifyCampaign(tenantId, campaignId);
        if (campaign.getStatus() == com.marketingagent.domain.campaign.AdHocCampaignStatus.PAUSED) {
            campaign.setStatus(com.marketingagent.domain.campaign.AdHocCampaignStatus.SCHEDULED);
            // If the original time passed, it will trigger immediately in Quartz
            scheduleJob(campaign);
        }
        return AdHocCampaignDto.from(adHocCampaignRepository.save(campaign));
    }

    @Transactional
    public AdHocCampaignDto cancelCampaign(UUID tenantId, UUID campaignId) {
        AdHocCampaign campaign = getAndVerifyCampaign(tenantId, campaignId);
        if (campaign.getStatus() == com.marketingagent.domain.campaign.AdHocCampaignStatus.SCHEDULED || 
            campaign.getStatus() == com.marketingagent.domain.campaign.AdHocCampaignStatus.PAUSED) {
            campaign.setStatus(com.marketingagent.domain.campaign.AdHocCampaignStatus.CANCELED);
            try {
                org.quartz.JobKey jobKey = org.quartz.JobKey.jobKey("adhoc-campaign-" + campaign.getId(), "adhoc-campaigns");
                scheduler.interrupt(jobKey);
                scheduler.deleteJob(jobKey);
            } catch (SchedulerException e) {
                LOGGER.error("Failed to cancel job in scheduler", e);
            }
        }
        return AdHocCampaignDto.from(adHocCampaignRepository.save(campaign));
    }

    @Transactional
    public AdHocCampaignDto rescheduleCampaign(UUID tenantId, UUID campaignId, Instant newTime) {
        AdHocCampaign campaign = getAndVerifyCampaign(tenantId, campaignId);
        if (campaign.getStatus() == com.marketingagent.domain.campaign.AdHocCampaignStatus.SCHEDULED || 
            campaign.getStatus() == com.marketingagent.domain.campaign.AdHocCampaignStatus.PAUSED) {
            
            campaign.setScheduledTime(newTime);
            campaign.setStatus(com.marketingagent.domain.campaign.AdHocCampaignStatus.SCHEDULED);
            try {
                org.quartz.TriggerKey triggerKey = org.quartz.TriggerKey.triggerKey("trigger-adhoc-campaign-" + campaign.getId(), "adhoc-campaigns");
                org.quartz.Trigger newTrigger = TriggerBuilder.newTrigger()
                        .withIdentity(triggerKey)
                        .startAt(Date.from(newTime))
                        .build();
                scheduler.rescheduleJob(triggerKey, newTrigger);
            } catch (SchedulerException e) {
                LOGGER.error("Failed to reschedule job", e);
                // If it fails (maybe deleted), try scheduling it anew
                try {
                    org.quartz.JobKey jobKey = org.quartz.JobKey.jobKey("adhoc-campaign-" + campaign.getId(), "adhoc-campaigns");
                    scheduler.deleteJob(jobKey);
                } catch (Exception ignore) {}
                scheduleJob(campaign);
            }
        }
        return AdHocCampaignDto.from(adHocCampaignRepository.save(campaign));
    }

    private AdHocCampaign getAndVerifyCampaign(UUID tenantId, UUID campaignId) {
        AdHocCampaign campaign = adHocCampaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("AdHocCampaign", campaignId));
        if (!campaign.getTenant().getId().equals(tenantId)) {
            throw new ResourceNotFoundException("AdHocCampaign", campaignId);
        }
        return campaign;
    }

    private void scheduleJob(AdHocCampaign campaign) {
        try {
            JobDetail jobDetail = JobBuilder.newJob(AdHocBroadcastJob.class)
                    .withIdentity("adhoc-campaign-" + campaign.getId(), "adhoc-campaigns")
                    .usingJobData("campaignId", campaign.getId().toString())
                    .build();

            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("trigger-adhoc-campaign-" + campaign.getId(), "adhoc-campaigns")
                    .startAt(Date.from(campaign.getScheduledTime()))
                    .build();

            scheduler.scheduleJob(jobDetail, trigger);
            LOGGER.info("Scheduled AdHocBroadcastJob for campaign {} at {}", campaign.getId(), campaign.getScheduledTime());
        } catch (SchedulerException e) {
            LOGGER.error("Failed to schedule AdHocBroadcastJob for campaign {}", campaign.getId(), e);
            throw new RuntimeException("Failed to schedule broadcast", e);
        }
    }
}
