package com.marketingagent.service;

import com.marketingagent.model.CalendarEntry;
import com.marketingagent.model.AdHocCampaign;
import com.marketingagent.model.Tenant;
import com.marketingagent.repository.CalendarEntryRepository;
import com.marketingagent.repository.AdHocCampaignRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BroadcastScheduler {

    @Autowired
    private CalendarEntryRepository calendarEntryRepository;

    @Autowired
    private AdHocCampaignRepository adHocCampaignRepository;

    @Autowired
    private WhatsAppService whatsAppService;

    @Autowired
    private StorageService storageService;

    @Scheduled(cron = "${marketing-agent.quartz.broadcast-scheduler-cron:0 */1 * * * ?}")
    @Transactional
    public void runBroadcastCycle() {
        System.out.println("[SCHEDULER] Checking for pending broadcasts...");

        // 1. Process Content Calendar Entries
        LocalDate today = LocalDate.now();
        List<CalendarEntry> dueCalendarEntries = calendarEntryRepository.findByStatusAndScheduledDateLessThanEqual("APPROVED", today);
        if (!dueCalendarEntries.isEmpty()) {
            System.out.println("[SCHEDULER] Found " + dueCalendarEntries.size() + " calendar posts due for broadcast.");
            for (CalendarEntry entry : dueCalendarEntries) {
                Tenant tenant = entry.getTenant();
                String mediaUrl = storageService.getFileUrl(entry.getMediaUrl());
                
                boolean sent = whatsAppService.sendBroadcast(
                        tenant.getWhatsappAccessToken(),
                        tenant.getWhatsappPhoneNumberId(),
                        entry.getMessageText(),
                        mediaUrl,
                        "+919307712930" // Default/mock recipient
                );

                entry.setStatus(sent ? "SENT" : "FAILED");
                entry.setSentAt(LocalDateTime.now());
                calendarEntryRepository.save(entry);
                System.out.println("[SCHEDULER] Post Day " + entry.getDayNumber() + " status updated to: " + entry.getStatus());
            }
        }

        // 2. Process Ad-Hoc Campaigns
        Instant now = Instant.now();
        List<AdHocCampaign> dueAdHoc = adHocCampaignRepository.findByStatusAndScheduledTimeLessThanEqual("SCHEDULED", now);
        if (!dueAdHoc.isEmpty()) {
            System.out.println("[SCHEDULER] Found " + dueAdHoc.size() + " ad-hoc campaigns due for broadcast.");
            for (AdHocCampaign campaign : dueAdHoc) {
                Tenant tenant = campaign.getTenant();
                String mediaUrl = storageService.getFileUrl(campaign.getMediaUrl());

                boolean sent = whatsAppService.sendBroadcast(
                        tenant.getWhatsappAccessToken(),
                        tenant.getWhatsappPhoneNumberId(),
                        campaign.getMessageText(),
                        mediaUrl,
                        "+919307712930"
                );

                campaign.setStatus(sent ? "SENT" : "FAILED");
                campaign.setSentAt(LocalDateTime.now());
                adHocCampaignRepository.save(campaign);
                System.out.println("[SCHEDULER] Ad-Hoc Campaign (" + campaign.getId() + ") status updated to: " + campaign.getStatus());
            }
        }
    }
}
