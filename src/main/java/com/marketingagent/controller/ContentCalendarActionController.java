package com.marketingagent.controller;

import com.marketingagent.dto.magazine.ContentCalendarDto;
import com.marketingagent.service.ContentCalendarService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/tenants/{tenantId}/calendar/{entryId}")
public class ContentCalendarActionController {

    private final ContentCalendarService contentCalendarService;

    public ContentCalendarActionController(ContentCalendarService contentCalendarService) {
        this.contentCalendarService = contentCalendarService;
    }

    @PostMapping("/approve")
    public ResponseEntity<ContentCalendarDto> approveEntry(
            @PathVariable UUID tenantId,
            @PathVariable UUID entryId) {
        return ResponseEntity.ok(contentCalendarService.approveEntry(tenantId, entryId));
    }

    @PostMapping("/on-hold")
    public ResponseEntity<ContentCalendarDto> onHoldEntry(
            @PathVariable UUID tenantId,
            @PathVariable UUID entryId) {
        return ResponseEntity.ok(contentCalendarService.onHoldEntry(tenantId, entryId));
    }

    @org.springframework.web.bind.annotation.DeleteMapping
    public ResponseEntity<Void> deleteEntry(
            @PathVariable UUID tenantId,
            @PathVariable UUID entryId) {
        contentCalendarService.deleteEntry(tenantId, entryId);
        return ResponseEntity.noContent().build();
    }

    public record EditMessageRequest(String messageText, String mediaUrl) {}

    @PutMapping("/message")
    public ResponseEntity<ContentCalendarDto> editEntryMessage(
            @PathVariable UUID tenantId,
            @PathVariable UUID entryId,
            @RequestBody EditMessageRequest request) {
        String cleanedText = request.messageText();
        if (cleanedText != null) {
            cleanedText = cleanedText.trim();
        }
        String cleanedUrl = request.mediaUrl();
        if (cleanedUrl != null) {
            cleanedUrl = cleanedUrl.trim();
        }
        return ResponseEntity.ok(contentCalendarService.editEntryMessage(tenantId, entryId, cleanedText, cleanedUrl));
    }

    @PostMapping("/regenerate")
    public ResponseEntity<ContentCalendarDto> regenerateEntry(
            @PathVariable UUID tenantId,
            @PathVariable UUID entryId) {
        return ResponseEntity.ok(contentCalendarService.regenerateEntry(tenantId, entryId));
    }

    @PostMapping(value = "/media", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ContentCalendarDto> uploadEntryMedia(
            @PathVariable UUID tenantId,
            @PathVariable UUID entryId,
            @org.springframework.web.bind.annotation.RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(contentCalendarService.uploadEntryMedia(tenantId, entryId, file));
    }

    @org.springframework.web.bind.annotation.GetMapping("/analytics")
    public ResponseEntity<com.marketingagent.dto.message.ContentAnalyticsDto> getAnalytics(
            @PathVariable UUID tenantId,
            @PathVariable UUID entryId) {
        return ResponseEntity.ok(contentCalendarService.getAnalytics(tenantId, entryId));
    }
}
