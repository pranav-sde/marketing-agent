package com.marketingagent.quartz;

import com.marketingagent.domain.campaign.AdHocCampaign;
import com.marketingagent.domain.campaign.AdHocCampaignStatus;
import com.marketingagent.domain.contact.Contact;
import com.marketingagent.domain.contact.ContactStatus;
import com.marketingagent.domain.magazine.ContentPlatform;
import com.marketingagent.domain.message.ContentOutboundMessage;
import com.marketingagent.domain.message.OutboundMessageStatus;
import com.marketingagent.webclient.whatsapp.WhatsAppMessageResponse;
import com.marketingagent.repository.AdHocCampaignRepository;
import com.marketingagent.repository.ContactRepository;
import com.marketingagent.repository.ContentOutboundMessageRepository;
import com.marketingagent.webclient.WhatsAppClientProperties;
import com.marketingagent.webclient.whatsapp.WhatsAppMessageClient;
import org.quartz.InterruptableJob;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class AdHocBroadcastJob implements InterruptableJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdHocBroadcastJob.class);

    private final AdHocCampaignRepository adHocCampaignRepository;
    private final ContactRepository contactRepository;
    private final ContentOutboundMessageRepository contentOutboundMessageRepository;
    private final WhatsAppMessageClient whatsAppMessageClient;
    private final WhatsAppClientProperties whatsAppClientProperties;

    private volatile boolean isInterrupted = false;

    public AdHocBroadcastJob(AdHocCampaignRepository adHocCampaignRepository,
                             ContactRepository contactRepository,
                             ContentOutboundMessageRepository contentOutboundMessageRepository,
                             WhatsAppMessageClient whatsAppMessageClient,
                             WhatsAppClientProperties whatsAppClientProperties) {
        this.adHocCampaignRepository = adHocCampaignRepository;
        this.contactRepository = contactRepository;
        this.contentOutboundMessageRepository = contentOutboundMessageRepository;
        this.whatsAppMessageClient = whatsAppMessageClient;
        this.whatsAppClientProperties = whatsAppClientProperties;
    }

    @Override
    public void interrupt() throws UnableToInterruptJobException {
        LOGGER.info("AdHocBroadcastJob was interrupted.");
        this.isInterrupted = true;
    }

    @Override
    @Transactional
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String campaignIdStr = context.getJobDetail().getJobDataMap().getString("campaignId");
        if (campaignIdStr == null) {
            LOGGER.error("No campaignId provided in JobDataMap");
            return;
        }

        UUID campaignId = UUID.fromString(campaignIdStr);
        AdHocCampaign campaign = adHocCampaignRepository.findById(campaignId).orElse(null);

        if (campaign == null) {
            LOGGER.error("AdHocCampaign {} not found", campaignId);
            return;
        }

        if (campaign.getStatus() != AdHocCampaignStatus.SCHEDULED) {
            LOGGER.info("AdHocCampaign {} is already processed (Status: {})", campaignId, campaign.getStatus());
            return;
        }

        LOGGER.info("Starting Broadcast for AdHocCampaign ID: {}", campaign.getId());

        com.marketingagent.domain.tenant.Tenant tenant = campaign.getTenant();
        String phoneId = tenant.getWhatsappPhoneNumberId();
        String accessToken = tenant.getWhatsappAccessToken();

        if (phoneId == null || phoneId.isBlank()) {
            phoneId = whatsAppClientProperties.getPhoneNumberId();
        }
        if (accessToken == null || accessToken.isBlank()) {
            accessToken = whatsAppClientProperties.getAccessToken();
        }

        int successCount = 0;
        int failCount = 0;
        int pageNumber = 0;
        int pageSize = 500;
        Page<Contact> page;

        do {
            if (isInterrupted) {
                LOGGER.info("Broadcast loop for AdHocCampaign {} interrupted mid-flight before page {}.", campaign.getId(), pageNumber);
                break;
            }

            Pageable pageable = PageRequest.of(pageNumber, pageSize);
            page = contactRepository.findByTenant_IdAndStatus(tenant.getId(), ContactStatus.ACTIVE, pageable);

            List<Contact> subscribers = page.getContent();
            if (subscribers.isEmpty()) {
                break;
            }

            // Filter out already processed (for resume functionality)
            List<ContentOutboundMessage> existingMessages = contentOutboundMessageRepository.findByAdHocCampaign_Id(campaign.getId());
            List<UUID> processedContactIds = existingMessages.stream().map(m -> m.getContact().getId()).collect(Collectors.toList());

            List<Contact> toProcess = subscribers.stream()
                    .filter(s -> !processedContactIds.contains(s.getId()))
                    .collect(Collectors.toList());

            if (!toProcess.isEmpty()) {
                // Initialize pending messages for the batch
                List<ContentOutboundMessage> pendingMessages = new ArrayList<>();
                for (Contact sub : toProcess) {
                    ContentOutboundMessage outboundMessage = new ContentOutboundMessage(tenant, sub, campaign.getPlatform());
                    outboundMessage.setAdHocCampaign(campaign);
                    outboundMessage.setStatus(OutboundMessageStatus.PENDING);
                    pendingMessages.add(outboundMessage);
                }

                // Save initially as PENDING
                pendingMessages = contentOutboundMessageRepository.saveAll(pendingMessages);

                final String finalPhoneId = phoneId;
                final String finalAccessToken = accessToken;

                // Process batch reactively with rate limiting
                List<ContentOutboundMessage> processedMessages = Flux.fromIterable(pendingMessages)
                        .delayElements(Duration.ofMillis(20)) // Enforce 50 requests per second rate limit
                        .flatMap(msg -> {
                            if (isInterrupted) {
                                return Mono.just(msg);
                            }
                            if (campaign.getPlatform() == ContentPlatform.WHATSAPP) {
                                return sendWhatsAppMessageReactive(campaign, msg.getContact(), finalPhoneId, finalAccessToken, msg)
                                        .onErrorResume(e -> {
                                            LOGGER.error("Failed to send message to contact {}", msg.getContact().getId(), e);
                                            msg.setStatus(OutboundMessageStatus.FAILED);
                                            msg.setLastErrorMessage(e.getMessage() != null && e.getMessage().length() > 500 ? e.getMessage().substring(0, 499) : e.getMessage());
                                            return Mono.just(msg);
                                        });
                            } else {
                                msg.setStatus(OutboundMessageStatus.FAILED);
                                msg.setLastErrorMessage("Platform not supported");
                                return Mono.just(msg);
                            }
                        })
                        .collectList()
                        .block();

                // Update counts and save all
                if (processedMessages != null) {
                    contentOutboundMessageRepository.saveAll(processedMessages);
                    for (ContentOutboundMessage msg : processedMessages) {
                        if (msg.getStatus() == OutboundMessageStatus.SENT) successCount++;
                        else if (msg.getStatus() == OutboundMessageStatus.FAILED) failCount++;
                    }
                }
            }

            pageNumber++;
        } while (page.hasNext() && !isInterrupted);

        if (isInterrupted) {
            LOGGER.info("Broadcast paused for AdHocCampaign {}. Success: {}, Failed: {}", campaign.getId(), successCount, failCount);
            campaign.setStatus(AdHocCampaignStatus.PAUSED);
        } else {
            LOGGER.info("Broadcast complete for AdHocCampaign {}. Success: {}, Failed: {}", campaign.getId(), successCount, failCount);
            campaign.setStatus(AdHocCampaignStatus.SENT);
        }

        adHocCampaignRepository.save(campaign);
    }

    private Mono<ContentOutboundMessage> sendWhatsAppMessageReactive(AdHocCampaign campaign, Contact subscriber, String phoneId, String accessToken, ContentOutboundMessage outboundMessage) {
        boolean hasMedia = campaign.getMediaUrl() != null && !campaign.getMediaUrl().isBlank();

        if (phoneId == null || phoneId.isBlank() || phoneId.contains("placeholder")) {
            LOGGER.warn("WhatsApp Phone Number ID not configured. Simulating send to {}", subscriber.getPhoneE164());
            outboundMessage.setProviderMessageId("mock-adhoc-wamid-" + UUID.randomUUID().toString());
            outboundMessage.setStatus(OutboundMessageStatus.SENT);
            outboundMessage.setSentAt(Instant.now());
            return Mono.just(outboundMessage);
        } else {
            Mono<WhatsAppMessageResponse> responseMono;
            if (hasMedia) {
                responseMono = whatsAppMessageClient.sendImageMessage(
                        accessToken,
                        phoneId,
                        subscriber.getPhoneE164(),
                        campaign.getMediaUrl(),
                        campaign.getMessageText() != null ? campaign.getMessageText() : ""
                );
            } else {
                responseMono = whatsAppMessageClient.sendTextMessage(
                        accessToken,
                        phoneId,
                        subscriber.getPhoneE164(),
                        campaign.getMessageText() != null ? campaign.getMessageText() : ""
                );
            }

            return responseMono.map(response -> {
                if (response != null && response.messages() != null && !response.messages().isEmpty()) {
                    outboundMessage.setProviderMessageId(response.messages().get(0).get("id").toString());
                }
                outboundMessage.setStatus(OutboundMessageStatus.SENT);
                outboundMessage.setSentAt(Instant.now());
                return outboundMessage;
            });
        }
    }
}
