package com.marketingagent.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketingagent.domain.magazine.ContentCalendar;
import com.marketingagent.domain.magazine.ContentCalendarStatus;
import com.marketingagent.domain.magazine.ContentPlatform;
import com.marketingagent.domain.magazine.GeneratedContent;
import com.marketingagent.domain.magazine.Magazine;
import com.marketingagent.domain.magazine.MagazineStatus;
import com.marketingagent.domain.magazine.Story;
import com.marketingagent.domain.audit.AuditActionType;
import com.marketingagent.domain.audit.AuditLog;
import com.marketingagent.repository.AuditLogRepository;
import com.marketingagent.repository.ContentCalendarRepository;
import com.marketingagent.repository.GeneratedContentRepository;
import com.marketingagent.repository.MagazineRepository;
import com.marketingagent.repository.StoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class StoryExtractionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StoryExtractionService.class);

    private final MagazineRepository magazineRepository;
    private final StoryRepository storyRepository;
    private final ContentCalendarRepository contentCalendarRepository;
    private final GeneratedContentRepository generatedContentRepository;
    private final ObjectMapper objectMapper;
    private final S3Service s3Service;
    private final PdfProcessingService pdfProcessingService;
    private final ContentGenerationService contentGenerationService;
    private final TransactionTemplate transactionTemplate;
    private final AuditLogRepository auditLogRepository;

    public StoryExtractionService(
            MagazineRepository magazineRepository,
            StoryRepository storyRepository,
            ContentCalendarRepository contentCalendarRepository,
            GeneratedContentRepository generatedContentRepository,
            ObjectMapper objectMapper,
            S3Service s3Service,
            PdfProcessingService pdfProcessingService,
            ContentGenerationService contentGenerationService,
            PlatformTransactionManager transactionManager,
            AuditLogRepository auditLogRepository) {
        this.magazineRepository = magazineRepository;
        this.storyRepository = storyRepository;
        this.contentCalendarRepository = contentCalendarRepository;
        this.generatedContentRepository = generatedContentRepository;
        this.objectMapper = objectMapper;
        this.s3Service = s3Service;
        this.pdfProcessingService = pdfProcessingService;
        this.contentGenerationService = contentGenerationService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Asynchronously process a magazine PDF.
     * Note: This method is NOT @Transactional at the class/method level to prevent holding database connections
     * during long network operations (S3 downloads and LLM calls). Database updates are isolated.
     */
    @Async
    public void extractStoriesAsync(UUID magazineId) {
        LOGGER.info("Starting async pipeline for magazine: {}", magazineId);

        // 1. Transition status to EXTRACTING
        Boolean started = transactionTemplate.execute(status -> {
            Magazine magazine = magazineRepository.findById(magazineId).orElse(null);
            if (magazine == null) {
                LOGGER.error("Magazine not found: {}", magazineId);
                return false;
            }
            magazine.setProcessingStatus(MagazineStatus.EXTRACTING);
            magazineRepository.save(magazine);

            // Audit log: PDF extraction started
            AuditLog log = new AuditLog(magazine.getTenant(), null, AuditActionType.PDF_EXTRACTION_STARTED, java.time.Instant.now());
            log.setEntityType("Magazine");
            log.setEntityId(magazineId);
            log.setDetails(java.util.Map.of("magazineTitle", magazine.getTitle()));
            auditLogRepository.save(log);

            return true;
        });

        if (started == null || !started) {
            return;
        }

        try {
            // Load magazine metadata
            Magazine magazine = transactionTemplate.execute(status -> magazineRepository.findById(magazineId).orElse(null));
            if (magazine == null) {
                throw new IllegalStateException("Magazine went missing during initialization: " + magazineId);
            }

            // 2. Validate S3 object metadata
            executeWithRetry("Validate storage metadata", () -> {
                s3Service.validateS3Object(magazine.getFilePath());
                return null;
            });

            // 3. Download/Stream and extract text from S3 directly without temp files
            String rawText = executeWithRetry("S3 stream & PDF extraction", () -> {
                try (InputStream s3Stream = s3Service.getObjectStream(magazine.getFilePath())) {
                    return pdfProcessingService.extractText(s3Stream);
                }
            });

            // 4. Generate structured 30-day content plan JSON via LLM
            String contentPlanJson = executeWithRetry("LLM content plan generation", () -> 
                contentGenerationService.generate30DayContentPlan(magazine.getTitle(), rawText)
            );

            // 5. Store structured JSON output in S3
            String s3PlanKey = "plans/" + magazine.getTenant().getId() + "/" + magazine.getId() + ".json";
            executeWithRetry("Upload content plan JSON to S3", () -> {
                s3Service.uploadFile(s3PlanKey, contentPlanJson.getBytes(StandardCharsets.UTF_8), "application/json");
                return null;
            });

            // 6. Parse JSON and populate Postgres database
            List<Map<String, Object>> planItems = objectMapper.readValue(contentPlanJson, new TypeReference<>() {});
            if (planItems.isEmpty()) {
                throw new RuntimeException("Generated content plan is empty.");
            }

            transactionTemplate.executeWithoutResult(status -> {
                // Fetch fresh magazine entity in current transaction
                Magazine currentMagazine = magazineRepository.findById(magazineId)
                        .orElseThrow(() -> new IllegalStateException("Magazine not found in transaction: " + magazineId));

                // Save raw text and plan JSON
                currentMagazine.setExtractedText(rawText);
                currentMagazine.setContentPlanJson(contentPlanJson);

                // Clean existing calendar/stories if any (for idempotency/re-run scenarios)
                List<ContentCalendar> existingCalendar = contentCalendarRepository.findByMagazine_IdOrderByDayNumberAsc(magazineId);
                if (!existingCalendar.isEmpty()) {
                    generatedContentRepository.deleteByCalendarEntry_Magazine_Id(magazineId);
                    contentCalendarRepository.deleteAll(existingCalendar);
                }
                storyRepository.deleteByMagazine_Id(magazineId);

                LocalDate startDate = LocalDate.now().plusDays(1);

                for (Map<String, Object> item : planItems) {
                    Integer dayNumber = ((Number) item.get("dayNumber")).intValue();
                    String storyTitle = (String) item.get("storyTitle");
                    String summary = (String) item.get("summary");
                    List<String> keywords = (List<String>) item.get("keywords");
                    String contentAngle = (String) item.get("contentAngle");
                    String postText = (String) item.get("postText");

                    // Create Story
                    Story story = new Story(currentMagazine.getTenant(), currentMagazine, storyTitle);
                    story.setSummary(summary);
                    story.setKeywords(keywords);
                    story.setContentAngle(contentAngle);
                    story = storyRepository.save(story);

                    // Create ContentCalendar entry
                    LocalDate scheduledDate = startDate.plusDays(dayNumber - 1);
                    ContentCalendar calendarEntry = new ContentCalendar(
                            currentMagazine.getTenant(),
                            currentMagazine,
                            story,
                            dayNumber,
                            scheduledDate
                    );
                    calendarEntry.setContentAngle(contentAngle);
                    calendarEntry.setStatus(ContentCalendarStatus.GENERATED);
                    calendarEntry = contentCalendarRepository.save(calendarEntry);

                    // Create GeneratedContent
                    GeneratedContent generatedContent = new GeneratedContent(
                            currentMagazine.getTenant(),
                            calendarEntry,
                            postText,
                            ContentPlatform.WHATSAPP
                    );
                    generatedContent.setHashtags(keywords);
                    generatedContentRepository.save(generatedContent);
                }

                currentMagazine.setProcessingStatus(MagazineStatus.PROCESSED);
                currentMagazine.setErrorMessage(null);
                magazineRepository.save(currentMagazine);

                // Audit log: PDF extraction success
                AuditLog log = new AuditLog(currentMagazine.getTenant(), null, AuditActionType.PDF_EXTRACTION_SUCCESS, java.time.Instant.now());
                log.setEntityType("Magazine");
                log.setEntityId(magazineId);
                log.setDetails(java.util.Map.of(
                    "magazineTitle", currentMagazine.getTitle(),
                    "message", "PDF processed and 30-day calendar generated successfully."
                ));
                auditLogRepository.save(log);
            });

            LOGGER.info("Successfully completed PDF processing pipeline for magazine: {}", magazineId);

        } catch (Exception e) {
            LOGGER.error("PDF processing pipeline failed for magazine: {}", magazineId, e);
            
            // Mark task as FAILED and log error message
            transactionTemplate.executeWithoutResult(status -> {
                Magazine currentMagazine = magazineRepository.findById(magazineId).orElse(null);
                if (currentMagazine != null) {
                    currentMagazine.setProcessingStatus(MagazineStatus.FAILED);
                    currentMagazine.setErrorMessage(e.getMessage());
                    magazineRepository.save(currentMagazine);

                    // Audit log: PDF extraction failed
                    AuditLog log = new AuditLog(currentMagazine.getTenant(), null, AuditActionType.PDF_EXTRACTION_FAILED, java.time.Instant.now());
                    log.setEntityType("Magazine");
                    log.setEntityId(magazineId);
                    log.setDetails(java.util.Map.of(
                        "magazineTitle", currentMagazine.getTitle(),
                        "error", e.getMessage() != null ? e.getMessage() : "Unknown error during extraction"
                    ));
                    auditLogRepository.save(log);
                }
            });
        }
    }

    private <T> T executeWithRetry(String operationName, RetryableCallable<T> callable) throws Exception {
        int maxAttempts = 3;
        int attempt = 1;
        long delayMs = 1500;

        while (true) {
            try {
                LOGGER.info("Executing: '{}' - Attempt {}/{}", operationName, attempt, maxAttempts);
                return callable.call();
            } catch (Exception e) {
                LOGGER.warn("Attempt {}/{} failed for '{}': {}", attempt, maxAttempts, operationName, e.getMessage());
                if (attempt >= maxAttempts) {
                    LOGGER.error("All {} attempts failed for '{}'", maxAttempts, operationName);
                    throw e;
                }
                attempt++;
                Thread.sleep(delayMs);
                delayMs *= 2; // exponential backoff
            }
        }
    }

    @FunctionalInterface
    private interface RetryableCallable<T> {
        T call() throws Exception;
    }
}
