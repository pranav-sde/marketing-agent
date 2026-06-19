package com.marketingagent.controller;

import com.marketingagent.domain.magazine.ContentCalendar;
import com.marketingagent.domain.magazine.ContentCalendarStatus;
import com.marketingagent.quartz.DailyContentBroadcastJob;
import com.marketingagent.repository.ContentCalendarRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/test")
public class TestController {

    private final ContentCalendarRepository contentCalendarRepository;
    private final DailyContentBroadcastJob dailyContentBroadcastJob;

    public TestController(ContentCalendarRepository contentCalendarRepository, DailyContentBroadcastJob dailyContentBroadcastJob) {
        this.contentCalendarRepository = contentCalendarRepository;
        this.dailyContentBroadcastJob = dailyContentBroadcastJob;
    }

    @PostMapping("/magazines/{magazineId}/trigger-broadcast")
    public ResponseEntity<String> triggerBroadcast(@PathVariable UUID magazineId) {
        // Find first calendar entry for this magazine
        List<ContentCalendar> entries = contentCalendarRepository.findByMagazine_IdOrderByDayNumberAsc(magazineId);
        if (entries.isEmpty()) {
            return ResponseEntity.badRequest().body("No content calendar entries found for magazine. Please generate a calendar first.");
        }

        // Set the first entry's scheduled date to today and status to GENERATED so the broadcast job picks it up
        ContentCalendar entry = entries.get(0);
        entry.setScheduledDate(LocalDate.now());
        entry.setStatus(ContentCalendarStatus.GENERATED);
        contentCalendarRepository.save(entry);

        try {
            // Run the DailyContentBroadcastJob manually
            dailyContentBroadcastJob.execute(null);
            return ResponseEntity.ok("Successfully scheduled day " + entry.getDayNumber() + " for today and ran the DailyContentBroadcastJob!");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to run broadcast job: " + e.getMessage());
        }
    }
}
