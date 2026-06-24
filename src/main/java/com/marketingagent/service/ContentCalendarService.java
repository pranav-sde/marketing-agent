package com.marketingagent.service;

import com.marketingagent.domain.magazine.ContentCalendar;
import com.marketingagent.domain.magazine.ContentCalendarStatus;
import com.marketingagent.domain.magazine.Magazine;
import com.marketingagent.domain.magazine.Story;
import com.marketingagent.dto.magazine.ContentCalendarDto;
import com.marketingagent.exception.ResourceNotFoundException;
import com.marketingagent.domain.magazine.GeneratedContent;
import com.marketingagent.domain.magazine.ContentPlatform;
import com.marketingagent.repository.GeneratedContentRepository;
import com.marketingagent.repository.ContentCalendarRepository;
import com.marketingagent.repository.ContentOutboundMessageRepository;
import com.marketingagent.repository.StoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ContentCalendarService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContentCalendarService.class);

    private final MagazineService magazineService;
    private final StoryRepository storyRepository;
    private final ContentCalendarRepository contentCalendarRepository;
    private final ContentGenerationService contentGenerationService;
    private final GeneratedContentRepository generatedContentRepository;
    private final StorageService storageService;
    private final ContentOutboundMessageRepository contentOutboundMessageRepository;

    public ContentCalendarService(
            MagazineService magazineService,
            StoryRepository storyRepository,
            ContentCalendarRepository contentCalendarRepository,
            ContentGenerationService contentGenerationService,
            GeneratedContentRepository generatedContentRepository,
            StorageService storageService,
            ContentOutboundMessageRepository contentOutboundMessageRepository) {
        this.magazineService = magazineService;
        this.storyRepository = storyRepository;
        this.contentCalendarRepository = contentCalendarRepository;
        this.contentGenerationService = contentGenerationService;
        this.generatedContentRepository = generatedContentRepository;
        this.storageService = storageService;
        this.contentOutboundMessageRepository = contentOutboundMessageRepository;
    }

    @Transactional
    public List<ContentCalendarDto> generateCalendar(UUID tenantId, UUID magazineId) {
        Magazine magazine = magazineService.getMagazineEntity(tenantId, magazineId);
        List<Story> stories = storyRepository.findByMagazine_Id(magazineId);

        if (stories.isEmpty()) {
            if (magazine.getProcessingStatus() == com.marketingagent.domain.magazine.MagazineStatus.EXTRACTING || 
                magazine.getProcessingStatus() == com.marketingagent.domain.magazine.MagazineStatus.UPLOADED) {
                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Magazine is still being processed by AI. Please wait 10-20 seconds and try again.");
            } else if (magazine.getProcessingStatus() == com.marketingagent.domain.magazine.MagazineStatus.FAILED) {
                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Story extraction failed: " + magazine.getErrorMessage());
            } else {
                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "No text/stories could be extracted from this PDF. Ensure it is not an image-only scanned PDF (Status: " + magazine.getProcessingStatus() + ").");
            }
        }

        // Clean existing calendar for this magazine if regenerating
        List<ContentCalendar> existing = contentCalendarRepository.findByMagazine_IdOrderByDayNumberAsc(magazineId);
        if (!existing.isEmpty()) {
            contentOutboundMessageRepository.deleteByCalendarEntry_Magazine_Id(magazineId);
            generatedContentRepository.deleteByCalendarEntry_Magazine_Id(magazineId);
            contentCalendarRepository.deleteAll(existing);
            contentCalendarRepository.flush();
        }

        List<ContentCalendar> calendarEntries = new ArrayList<>();
        LocalDate startDate = LocalDate.now().plusDays(1); // Start tomorrow

        // Distribute stories across 30 days using a simple round-robin
        for (int i = 0; i < 30; i++) {
            Story selectedStory = stories.get(i % stories.size());
            LocalDate scheduledDate = startDate.plusDays(i);

            ContentCalendar entry = new ContentCalendar(
                    magazine.getTenant(),
                    magazine,
                    selectedStory,
                    i + 1,
                    scheduledDate
            );
            entry.setContentAngle(selectedStory.getContentAngle());
            calendarEntries.add(entry);
        }

        calendarEntries = contentCalendarRepository.saveAll(calendarEntries);
        LOGGER.info("Generated 30-day content calendar for magazine {}", magazineId);

        // Async trigger generation of the actual content posts for all 30 days after transaction commits
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    contentGenerationService.generateContentForCalendarAsync(magazineId);
                }
            });
        } else {
            contentGenerationService.generateContentForCalendarAsync(magazineId);
        }

        return calendarEntries.stream().map(ContentCalendarDto::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ContentCalendarDto> getCalendar(UUID tenantId, UUID magazineId) {
        // Validate tenant owns magazine
        magazineService.getMagazineEntity(tenantId, magazineId);
        return contentCalendarRepository.findByMagazine_IdOrderByDayNumberAsc(magazineId)
                .stream()
                .map(entry -> {
                    GeneratedContent content = generatedContentRepository.findByCalendarEntry_IdAndPlatform(entry.getId(), ContentPlatform.WHATSAPP)
                            .orElse(null);
                    String messageText = content != null ? content.getMessageText() : null;
                    String mediaUrl = content != null ? content.getMediaUrl() : null;
                    return ContentCalendarDto.from(entry, messageText, mediaUrl);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ContentCalendarDto getDayContent(UUID tenantId, UUID magazineId, Integer dayNumber) {
        return getCalendar(tenantId, magazineId).stream()
                .filter(entry -> entry.dayNumber().equals(dayNumber))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("ContentCalendar day", UUID.randomUUID()));
    }

    @Transactional
    public ContentCalendarDto approveEntry(UUID tenantId, UUID entryId) {
        ContentCalendar entry = contentCalendarRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("ContentCalendar entry", entryId));
        if (!entry.getTenant().getId().equals(tenantId)) {
            throw new ResourceNotFoundException("ContentCalendar entry", entryId);
        }
        entry.setStatus(ContentCalendarStatus.APPROVED);
        ContentCalendar saved = contentCalendarRepository.save(entry);
        
        GeneratedContent content = generatedContentRepository.findByCalendarEntry_IdAndPlatform(entryId, ContentPlatform.WHATSAPP)
                .orElse(null);
        String messageText = content != null ? content.getMessageText() : null;
        String mediaUrl = content != null ? content.getMediaUrl() : null;
        return ContentCalendarDto.from(saved, messageText, mediaUrl);
    }

    @Transactional
    public ContentCalendarDto onHoldEntry(UUID tenantId, UUID entryId) {
        ContentCalendar entry = contentCalendarRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("ContentCalendar entry", entryId));
        if (!entry.getTenant().getId().equals(tenantId)) {
            throw new ResourceNotFoundException("ContentCalendar entry", entryId);
        }
        entry.setStatus(ContentCalendarStatus.ON_HOLD);
        ContentCalendar saved = contentCalendarRepository.save(entry);
        
        GeneratedContent content = generatedContentRepository.findByCalendarEntry_IdAndPlatform(entryId, ContentPlatform.WHATSAPP)
                .orElse(null);
        String messageText = content != null ? content.getMessageText() : null;
        String mediaUrl = content != null ? content.getMediaUrl() : null;
        return ContentCalendarDto.from(saved, messageText, mediaUrl);
    }

    @Transactional
    public void deleteEntry(UUID tenantId, UUID entryId) {
        ContentCalendar entry = contentCalendarRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("ContentCalendar entry", entryId));
        if (!entry.getTenant().getId().equals(tenantId)) {
            throw new ResourceNotFoundException("ContentCalendar entry", entryId);
        }
        
        List<GeneratedContent> generated = generatedContentRepository.findByCalendarEntry_Id(entryId);
        if (!generated.isEmpty()) {
            generatedContentRepository.deleteAll(generated);
        }
        contentCalendarRepository.delete(entry);
    }

    @Transactional
    public ContentCalendarDto editEntryMessage(UUID tenantId, UUID entryId, String newMessageText, String newMediaUrl) {
        ContentCalendar entry = contentCalendarRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("ContentCalendar entry", entryId));
        if (!entry.getTenant().getId().equals(tenantId)) {
            throw new ResourceNotFoundException("ContentCalendar entry", entryId);
        }
        
        GeneratedContent generated = generatedContentRepository.findByCalendarEntry_IdAndPlatform(entryId, ContentPlatform.WHATSAPP)
                .orElseThrow(() -> new IllegalStateException("No content generated for this entry yet."));
        
        generated.setMessageText(newMessageText);
        generated.setMediaUrl(newMediaUrl);
        generatedContentRepository.save(generated);
        
        return ContentCalendarDto.from(entry, newMessageText, newMediaUrl);
    }

    @Transactional
    public ContentCalendarDto regenerateEntry(UUID tenantId, UUID entryId) {
        ContentCalendar entry = contentCalendarRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("ContentCalendar entry", entryId));
        if (!entry.getTenant().getId().equals(tenantId)) {
            throw new ResourceNotFoundException("ContentCalendar entry", entryId);
        }
        
        contentGenerationService.regeneratePlatformContent(entryId, ContentPlatform.WHATSAPP);
        
        // Refresh the entity state
        ContentCalendar refreshedEntry = contentCalendarRepository.findById(entryId).orElse(entry);
        GeneratedContent content = generatedContentRepository.findByCalendarEntry_IdAndPlatform(entryId, ContentPlatform.WHATSAPP)
                .orElse(null);
        String messageText = content != null ? content.getMessageText() : null;
        String mediaUrl = content != null ? content.getMediaUrl() : null;
        return ContentCalendarDto.from(refreshedEntry, messageText, mediaUrl);
    }

    @Transactional
    public ContentCalendarDto uploadEntryMedia(UUID tenantId, UUID entryId, org.springframework.web.multipart.MultipartFile file) {
        ContentCalendar entry = contentCalendarRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("ContentCalendar entry", entryId));
        if (!entry.getTenant().getId().equals(tenantId)) {
            throw new ResourceNotFoundException("ContentCalendar entry", entryId);
        }

        GeneratedContent generated = generatedContentRepository.findByCalendarEntry_IdAndPlatform(entryId, ContentPlatform.WHATSAPP)
                .orElseThrow(() -> new IllegalStateException("No content generated for this entry yet."));

        try {
            String extension = "jpg";
            if (file.getOriginalFilename() != null && file.getOriginalFilename().toLowerCase().endsWith(".png")) {
                extension = "png";
            }
            String key = "custom-media-" + UUID.randomUUID() + "." + extension;
            String mediaUrl = storageService.uploadFile(key, file.getBytes(), file.getContentType());
            generated.setMediaUrl(mediaUrl);
            generatedContentRepository.save(generated);
            
            return ContentCalendarDto.from(entry, generated.getMessageText(), mediaUrl);
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to read file", e);
        }
    }

    @Transactional(readOnly = true)
    public com.marketingagent.dto.message.ContentAnalyticsDto getAnalytics(UUID tenantId, UUID entryId) {
        ContentCalendar entry = contentCalendarRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("ContentCalendar entry", entryId));
        if (!entry.getTenant().getId().equals(tenantId)) {
            throw new ResourceNotFoundException("ContentCalendar entry", entryId);
        }

        List<Object[]> counts = contentOutboundMessageRepository.countStatusByCalendarEntryId(entryId);
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
                default: break; // Ignore PENDING, QUEUED etc
            }
        }
        return new com.marketingagent.dto.message.ContentAnalyticsDto(sent, delivered, read, failed);
    }
}
