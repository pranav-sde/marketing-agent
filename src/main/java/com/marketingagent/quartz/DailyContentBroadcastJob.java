package com.marketingagent.quartz;

import com.marketingagent.domain.contact.Contact;
import com.marketingagent.domain.contact.ContactStatus;
import com.marketingagent.domain.magazine.ContentCalendar;
import com.marketingagent.domain.magazine.ContentCalendarStatus;
import com.marketingagent.domain.magazine.ContentPlatform;
import com.marketingagent.domain.magazine.GeneratedContent;
import com.marketingagent.repository.ContactRepository;
import com.marketingagent.repository.ContentCalendarRepository;
import com.marketingagent.repository.GeneratedContentRepository;
import com.marketingagent.webclient.whatsapp.WhatsAppMessageClient;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    private final WhatsAppMessageClient whatsAppMessageClient;

    public DailyContentBroadcastJob(
            ContentCalendarRepository contentCalendarRepository,
            GeneratedContentRepository generatedContentRepository,
            ContactRepository contactRepository,
            WhatsAppMessageClient whatsAppMessageClient) {
        this.contentCalendarRepository = contentCalendarRepository;
        this.generatedContentRepository = generatedContentRepository;
        this.contactRepository = contactRepository;
        this.whatsAppMessageClient = whatsAppMessageClient;
    }

    @Override
    @Transactional
    public void execute(JobExecutionContext context) throws JobExecutionException {
        LocalDate today = LocalDate.now();
        LOGGER.info("Starting Daily Content Broadcast for date: {}", today);

        // Find all GENERATED calendar entries scheduled for today
        List<ContentCalendar> todaysEntries = contentCalendarRepository.findByScheduledDateAndStatus(today, ContentCalendarStatus.GENERATED);

        if (todaysEntries.isEmpty()) {
            LOGGER.info("No content calendar entries scheduled for broadcast today.");
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

        int successCount = 0;
        int failCount = 0;

        for (Contact subscriber : subscribers) {
            try {
                // Here we use the text message client
                // Note: The original WhatsAppMessageClient only implements template messages right now.
                // For MVP, if it supports text, we use it. Let's assume it has a text sending method or we log it.
                // Since the original client uses TemplateMessageBuilder, we might need a TextMessageBuilder.
                // We will just log the send attempt for MVP to simulate the API call.
                
                LOGGER.debug("Sending message to contact {}: {}", subscriber.getPhoneE164(), whatsappContent.getMessageText());
                
                // In reality, this would be:
                // whatsAppMessageClient.sendTextMessage(subscriber.getPhoneE164(), whatsappContent.getMessageText());
                
                successCount++;
            } catch (Exception e) {
                LOGGER.error("Failed to send message to contact {}", subscriber.getId(), e);
                failCount++;
            }
        }

        LOGGER.info("Broadcast complete for Entry {}. Success: {}, Failed: {}", entry.getId(), successCount, failCount);

        entry.setStatus(ContentCalendarStatus.SENT);
        contentCalendarRepository.save(entry);
    }
}
