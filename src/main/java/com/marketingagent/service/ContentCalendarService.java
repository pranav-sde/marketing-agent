package com.marketingagent.service;

import com.marketingagent.domain.magazine.ContentCalendar;
import com.marketingagent.domain.magazine.Magazine;
import com.marketingagent.domain.magazine.Story;
import com.marketingagent.dto.magazine.ContentCalendarDto;
import com.marketingagent.exception.ResourceNotFoundException;
import com.marketingagent.domain.magazine.GeneratedContent;
import com.marketingagent.domain.magazine.ContentPlatform;
import com.marketingagent.repository.GeneratedContentRepository;
import com.marketingagent.repository.ContentCalendarRepository;
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

    public ContentCalendarService(
            MagazineService magazineService,
            StoryRepository storyRepository,
            ContentCalendarRepository contentCalendarRepository,
            ContentGenerationService contentGenerationService,
            GeneratedContentRepository generatedContentRepository) {
        this.magazineService = magazineService;
        this.storyRepository = storyRepository;
        this.contentCalendarRepository = contentCalendarRepository;
        this.contentGenerationService = contentGenerationService;
        this.generatedContentRepository = generatedContentRepository;
    }

    @Transactional
    public List<ContentCalendarDto> generateCalendar(UUID tenantId, UUID magazineId) {
        Magazine magazine = magazineService.getMagazineEntity(tenantId, magazineId);
        List<Story> stories = storyRepository.findByMagazine_Id(magazineId);

        if (stories.isEmpty()) {
            throw new IllegalStateException("No stories extracted from this magazine yet.");
        }

        // Clean existing calendar for this magazine if regenerating
        List<ContentCalendar> existing = contentCalendarRepository.findByMagazine_IdOrderByDayNumberAsc(magazineId);
        if (!existing.isEmpty()) {
            for (ContentCalendar entry : existing) {
                List<GeneratedContent> generated = generatedContentRepository.findByCalendarEntry_Id(entry.getId());
                if (!generated.isEmpty()) {
                    generatedContentRepository.deleteAll(generated);
                }
            }
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
                    String messageText = generatedContentRepository.findByCalendarEntry_IdAndPlatform(entry.getId(), ContentPlatform.WHATSAPP)
                            .map(GeneratedContent::getMessageText)
                            .orElse(null);
                    return ContentCalendarDto.from(entry, messageText);
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
}
