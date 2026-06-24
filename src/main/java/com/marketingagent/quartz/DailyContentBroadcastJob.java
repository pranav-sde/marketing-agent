package com.marketingagent.quartz;

import com.marketingagent.domain.contact.Contact;
import com.marketingagent.domain.contact.ContactStatus;
import com.marketingagent.domain.magazine.ContentCalendar;
import com.marketingagent.domain.magazine.ContentCalendarStatus;
import com.marketingagent.domain.magazine.ContentPlatform;
import com.marketingagent.domain.magazine.GeneratedContent;
import com.marketingagent.domain.message.ContentOutboundMessage;
import com.marketingagent.domain.message.OutboundMessageStatus;
import com.marketingagent.repository.ContactRepository;
import com.marketingagent.repository.ContentCalendarRepository;
import com.marketingagent.repository.ContentOutboundMessageRepository;
import com.marketingagent.repository.GeneratedContentRepository;
import com.marketingagent.repository.AuditLogRepository;
import com.marketingagent.domain.audit.AuditActionType;
import com.marketingagent.domain.audit.AuditLog;
import com.marketingagent.webclient.WhatsAppClientProperties;
import com.marketingagent.webclient.whatsapp.WhatsAppMessageClient;
import com.marketingagent.webclient.whatsapp.WhatsAppMessageResponse;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Job that runs daily at 9 AM to broadcast WhatsApp messages for today's Content Calendar entries.
 */
@Component
public class DailyContentBroadcastJob implements Job {

    private static final Logger LOGGER = LoggerFactory.getLogger(DailyContentBroadcastJob.class);

    private final ContentCalendarRepository contentCalendarRepository;
    private final GeneratedContentRepository generatedContentRepository;
    private final ContactRepository contactRepository;
    private final ContentOutboundMessageRepository contentOutboundMessageRepository;
    private final WhatsAppMessageClient whatsAppMessageClient;
    private final WhatsAppClientProperties whatsAppClientProperties;
    private final AuditLogRepository auditLogRepository;

    public DailyContentBroadcastJob(
            ContentCalendarRepository contentCalendarRepository,
            GeneratedContentRepository generatedContentRepository,
            ContactRepository contactRepository,
            ContentOutboundMessageRepository contentOutboundMessageRepository,
            WhatsAppMessageClient whatsAppMessageClient,
            WhatsAppClientProperties whatsAppClientProperties,
            AuditLogRepository auditLogRepository) {
        this.contentCalendarRepository = contentCalendarRepository;
        this.generatedContentRepository = generatedContentRepository;
        this.contactRepository = contactRepository;
        this.contentOutboundMessageRepository = contentOutboundMessageRepository;
        this.whatsAppMessageClient = whatsAppMessageClient;
        this.whatsAppClientProperties = whatsAppClientProperties;
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    @Transactional
    public void execute(JobExecutionContext context) throws JobExecutionException {
        LocalDate today = LocalDate.now();
        LOGGER.info("Starting Daily Content Broadcast for date: {}", today);

        // Find all APPROVED calendar entries scheduled for today
        List<ContentCalendar> todaysEntries = contentCalendarRepository.findByScheduledDateAndStatus(today, ContentCalendarStatus.APPROVED);

        if (todaysEntries.isEmpty()) {
            LOGGER.info("No approved content calendar entries scheduled for broadcast today.");
            return;
        }

        for (ContentCalendar entry : todaysEntries) {
            broadcastForEntry(entry);
        }
    }

    private void broadcastForEntry(ContentCalendar entry) {
        LOGGER.info("Broadcasting for Calendar Entry ID: {}", entry.getId());
        
        GeneratedContent whatsappContent = generatedContentRepository
                .findByCalendarEntry_IdAndPlatform(entry.getId(), ContentPlatform.WHATSAPP)
                .orElse(null);

        if (whatsappContent == null) {
            LOGGER.warn("No WhatsApp generated content found for Calendar Entry ID: {}", entry.getId());
            return;
        }

        // Fetch all active contacts
        List<Contact> subscribers = contactRepository.findByTenant_IdAndStatus(entry.getTenant().getId(), ContactStatus.ACTIVE);

        LOGGER.info("Found {} active subscribers for Tenant {}", subscribers.size(), entry.getTenant().getId());

        // Resolve credentials per tenant
        com.marketingagent.domain.tenant.Tenant tenant = entry.getTenant();
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

        for (Contact subscriber : subscribers) {
            ContentOutboundMessage outboundMessage = new ContentOutboundMessage(tenant, subscriber, ContentPlatform.WHATSAPP);
            outboundMessage.setCalendarEntry(entry);
            outboundMessage.setStatus(OutboundMessageStatus.PENDING);
            outboundMessage = contentOutboundMessageRepository.save(outboundMessage);

            try {
                String mediaUrl = whatsappContent.getMediaUrl();
                boolean hasMedia = mediaUrl != null && !mediaUrl.isBlank();
                
                LOGGER.debug("Sending message to contact {}: {}. Has media: {}", 
                        subscriber.getPhoneE164(), whatsappContent.getMessageText(), hasMedia);
                
                WhatsAppMessageResponse response = null;

                if (phoneId == null || phoneId.isBlank() || phoneId.contains("placeholder")) {
                    phoneId = "default_phone_number_id"; // Mock behavior if not configured
                    LOGGER.warn("WhatsApp Phone Number ID not configured. Simulating send to {} (media: {})", 
                            subscriber.getPhoneE164(), mediaUrl);
                    outboundMessage.setProviderMessageId("mock-wamid-" + java.util.UUID.randomUUID().toString());
                } else {
                    if (hasMedia) {
                        response = whatsAppMessageClient.sendImageMessage(
                                accessToken,
                                phoneId,
                                subscriber.getPhoneE164(),
                                mediaUrl,
                                whatsappContent.getMessageText()
                        ).block();
                    } else {
                        response = whatsAppMessageClient.sendTextMessage(
                                accessToken,
                                phoneId,
                                subscriber.getPhoneE164(),
                                whatsappContent.getMessageText()
                        ).block();
                    }
                    if (response != null && response.messages() != null && !response.messages().isEmpty()) {
                        outboundMessage.setProviderMessageId(response.messages().get(0).get("id").toString());
                    }
                }
                
                outboundMessage.setStatus(OutboundMessageStatus.SENT);
                outboundMessage.setSentAt(Instant.now());
                outboundMessage = contentOutboundMessageRepository.save(outboundMessage);
                successCount++;

                // Audit log: WhatsApp message sent successfully
                AuditLog log = new AuditLog(tenant, null, AuditActionType.WHATSAPP_MESSAGE_SENT, java.time.Instant.now());
                log.setEntityType("ContentOutboundMessage");
                log.setEntityId(outboundMessage.getId());
                log.setDetails(java.util.Map.of(
                    "contactPhone", subscriber.getPhoneE164(),
                    "contactName", subscriber.getFirstName() != null ? subscriber.getFirstName() : "",
                    "messageText", whatsappContent.getMessageText() != null ? whatsappContent.getMessageText() : "",
                    "type", "DailyBroadcast",
                    "calendarEntryId", entry.getId().toString()
                ));
                auditLogRepository.save(log);
            } catch (Exception e) {
                LOGGER.error("Failed to send message to contact {}", subscriber.getId(), e);
                outboundMessage.setStatus(OutboundMessageStatus.FAILED);
                outboundMessage.setLastErrorMessage(e.getMessage() != null && e.getMessage().length() > 500 ? e.getMessage().substring(0, 499) : e.getMessage());
                outboundMessage = contentOutboundMessageRepository.save(outboundMessage);
                failCount++;

                // Audit log: WhatsApp message sending failed
                AuditLog log = new AuditLog(tenant, null, AuditActionType.WHATSAPP_MESSAGE_FAILED, java.time.Instant.now());
                log.setEntityType("ContentOutboundMessage");
                log.setEntityId(outboundMessage.getId());
                log.setDetails(java.util.Map.of(
                    "contactPhone", subscriber.getPhoneE164(),
                    "contactName", subscriber.getFirstName() != null ? subscriber.getFirstName() : "",
                    "messageText", whatsappContent.getMessageText() != null ? whatsappContent.getMessageText() : "",
                    "type", "DailyBroadcast",
                    "calendarEntryId", entry.getId().toString(),
                    "error", e.getMessage() != null ? e.getMessage() : "Unknown error"
                ));
                auditLogRepository.save(log);
            }
        }

        LOGGER.info("Broadcast complete for Entry {}. Success: {}, Failed: {}", entry.getId(), successCount, failCount);

        entry.setStatus(ContentCalendarStatus.SENT);
        contentCalendarRepository.save(entry);
    }
}
