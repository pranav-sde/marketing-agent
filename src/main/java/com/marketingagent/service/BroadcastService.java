package com.marketingagent.service;

import com.marketingagent.domain.audience.AudienceSnapshot;
import com.marketingagent.domain.broadcast.Broadcast;
import com.marketingagent.domain.broadcast.BroadcastStatus;
import com.marketingagent.domain.campaign.Campaign;
import com.marketingagent.domain.campaign.CampaignStatus;
import com.marketingagent.domain.integration.WhatsAppAccount;
import com.marketingagent.domain.template.MessageTemplate;
import com.marketingagent.domain.template.TemplateStatus;
import com.marketingagent.domain.template.TemplateVersion;
import com.marketingagent.dto.broadcast.BroadcastCreateRequest;
import com.marketingagent.dto.broadcast.BroadcastDto;
import com.marketingagent.exception.BadRequestException;
import com.marketingagent.exception.ResourceNotFoundException;
import com.marketingagent.repository.BroadcastRepository;
import com.marketingagent.repository.TemplateVersionRepository;
import com.marketingagent.repository.WhatsAppAccountRepository;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class BroadcastService {

    private final CampaignService campaignService;
    private final AudienceService audienceService;
    private final TemplateService templateService;
    private final TemplateVersionRepository templateVersionRepository;
    private final WhatsAppAccountRepository whatsAppAccountRepository;
    private final BroadcastRepository broadcastRepository;

    public BroadcastService(
            CampaignService campaignService,
            AudienceService audienceService,
            TemplateService templateService,
            TemplateVersionRepository templateVersionRepository,
            WhatsAppAccountRepository whatsAppAccountRepository,
            BroadcastRepository broadcastRepository
    ) {
        this.campaignService = campaignService;
        this.audienceService = audienceService;
        this.templateService = templateService;
        this.templateVersionRepository = templateVersionRepository;
        this.whatsAppAccountRepository = whatsAppAccountRepository;
        this.broadcastRepository = broadcastRepository;
    }

    @Transactional
    public BroadcastDto createBroadcast(UUID tenantId, @Valid BroadcastCreateRequest request) {
        Campaign campaign = campaignService.getCampaignEntity(tenantId, request.campaignId());
        if (campaign.getStatus() != CampaignStatus.APPROVED) {
            throw new BadRequestException("Campaign must be approved before creating a broadcast");
        }

        AudienceSnapshot snapshot = audienceService.getSnapshotEntity(tenantId, request.audienceSnapshotId());
        MessageTemplate template = templateService.getTemplateEntity(tenantId, request.messageTemplateId());
        if (template.getStatus() != TemplateStatus.APPROVED) {
            throw new BadRequestException("WhatsApp template must be approved before broadcast");
        }

        TemplateVersion version = templateVersionRepository.findById(request.templateVersionId())
                .orElseThrow(() -> new ResourceNotFoundException("TemplateVersion", request.templateVersionId()));
        WhatsAppAccount account = whatsAppAccountRepository.findById(request.whatsAppAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("WhatsAppAccount", request.whatsAppAccountId()));
        if (!version.getTenant().getId().equals(tenantId) || !account.getTenant().getId().equals(tenantId)) {
            throw new ResourceNotFoundException("Broadcast dependency not found for tenant");
        }

        Broadcast broadcast = new Broadcast(tenantId == null ? null : campaign.getTenant(), campaign, snapshot, template, version, account);
        broadcast.setScheduledStartAt(request.scheduledStartAt());
        broadcast.setScheduledEndAt(request.scheduledEndAt());
        if (request.timezone() != null) {
            broadcast.setTimezone(request.timezone());
        }
        if (request.rateLimitProfile() != null) {
            broadcast.setRateLimitProfile(request.rateLimitProfile());
        }
        broadcast.setStatus(request.scheduledStartAt() == null ? BroadcastStatus.APPROVED : BroadcastStatus.SCHEDULED);
        return BroadcastDto.from(broadcastRepository.save(broadcast));
    }

    @Transactional(readOnly = true)
    public Broadcast getBroadcastEntity(UUID tenantId, UUID broadcastId) {
        Broadcast broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new ResourceNotFoundException("Broadcast", broadcastId));
        if (!broadcast.getTenant().getId().equals(tenantId)) {
            throw new ResourceNotFoundException("Broadcast", broadcastId);
        }
        return broadcast;
    }

    @Transactional
    public BroadcastDto startBroadcast(UUID tenantId, UUID broadcastId) {
        Broadcast broadcast = getBroadcastEntity(tenantId, broadcastId);
        if (broadcast.getStatus() != BroadcastStatus.APPROVED && broadcast.getStatus() != BroadcastStatus.SCHEDULED) {
            throw new BadRequestException("Only approved or scheduled broadcasts can be started");
        }
        broadcast.setStatus(BroadcastStatus.QUEUED);
        broadcast.setLockedAt(Instant.now());
        return BroadcastDto.from(broadcastRepository.save(broadcast));
    }

    @Transactional
    public BroadcastDto pauseBroadcast(UUID tenantId, UUID broadcastId) {
        Broadcast broadcast = getBroadcastEntity(tenantId, broadcastId);
        if (broadcast.getStatus() != BroadcastStatus.QUEUED && broadcast.getStatus() != BroadcastStatus.SENDING) {
            throw new BadRequestException("Only queued or sending broadcasts can be paused");
        }
        broadcast.setStatus(BroadcastStatus.PAUSED);
        return BroadcastDto.from(broadcastRepository.save(broadcast));
    }

    @Transactional
    public BroadcastDto cancelBroadcast(UUID tenantId, UUID broadcastId) {
        Broadcast broadcast = getBroadcastEntity(tenantId, broadcastId);
        if (broadcast.getStatus() == BroadcastStatus.COMPLETED || broadcast.getStatus() == BroadcastStatus.CANCELED) {
            throw new BadRequestException("Completed or canceled broadcasts cannot be canceled again");
        }
        broadcast.setStatus(BroadcastStatus.CANCELED);
        return BroadcastDto.from(broadcastRepository.save(broadcast));
    }

    @Transactional
    public int queueDueBroadcasts(Instant now) {
        List<Broadcast> due = broadcastRepository.findByStatusAndScheduledStartAtLessThanEqual(
                BroadcastStatus.SCHEDULED,
                now
        );
        due.forEach(broadcast -> {
            broadcast.setStatus(BroadcastStatus.QUEUED);
            broadcast.setLockedAt(now);
        });
        broadcastRepository.saveAll(due);
        return due.size();
    }
}
