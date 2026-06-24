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

import java.io.File;
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
    private final MediaGenerationService mediaGenerationService;

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
            AuditLogRepository auditLogRepository,
            MediaGenerationService mediaGenerationService) {
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
        this.mediaGenerationService = mediaGenerationService;
    }

    /**
     * Asynchronously process a magazine PDF using file stored at magazine.getFilePath().
     */
    @Async
    public void extractStoriesAsync(UUID magazineId) {
        LOGGER.info("Starting async pipeline for magazine: {}", magazineId);
        // Retrieve file path from DB record
        String filePath = transactionTemplate.execute(status -> {
            Magazine m = magazineRepository.findById(magazineId).orElse(null);
            return m != null ? m.getFilePath() : null;
        });
        extractStoriesAsyncFromFile(magazineId, filePath);
    }

    /**
     * Asynchronously process a magazine PDF from an explicit file path (used for retries).
     */
    @Async
    public void extractStoriesAsyncFromFile(UUID magazineId, String explicitFilePath) {
        LOGGER.info("Starting async pipeline for magazine: {} with file: {}", magazineId, explicitFilePath);


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

            File localFile = new File(explicitFilePath != null ? explicitFilePath : magazine.getFilePath());
            if (!localFile.exists()) {
                throw new RuntimeException("Local file not found for extraction: " + localFile.getAbsolutePath());
            }

            // 2. Extract text natively from local file
            String rawText = executeWithRetry("Local PDF extraction", () -> {
                try (InputStream fileStream = new java.io.FileInputStream(localFile)) {
                    return pdfProcessingService.extractText(fileStream);
                }
            });

            // 3. Preload all necessary media thumbnails and upload to S3
            executeWithRetry("Preload media thumbnails", () -> {
                mediaGenerationService.preloadMagazineMedia(magazine, localFile);
                return null;
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

                int fallbackDayNumber = 1;
                for (Map<String, Object> item : planItems) {
                    Number dayNumObj = null;
                    if (item.get("dayNumber") instanceof Number) {
                        dayNumObj = (Number) item.get("dayNumber");
                    } else if (item.get("day_number") instanceof Number) {
                        dayNumObj = (Number) item.get("day_number");
                    } else if (item.get("day") instanceof Number) {
                        dayNumObj = (Number) item.get("day");
                    }
                    
                    int dayNumber = dayNumObj != null ? dayNumObj.intValue() : fallbackDayNumber;
                    fallbackDayNumber = dayNumber + 1;

                    String storyTitle = item.get("storyTitle") != null ? item.get("storyTitle").toString() : 
                                       item.get("title") != null ? item.get("title").toString() : "Untitled Story";
                                       
                    String summary = item.get("summary") != null ? item.get("summary").toString() : "";
                    
                    List<String> keywords = List.of();
                    Object keywordsObj = item.get("keywords");
                    if (keywordsObj instanceof List) {
                        keywords = ((List<?>) keywordsObj).stream().map(Object::toString).toList();
                    } else if (keywordsObj instanceof String) {
                        keywords = List.of(((String) keywordsObj).split(",\\s*"));
                    }
                    
                    String contentAngle = item.get("contentAngle") != null ? item.get("contentAngle").toString() : 
                                         item.get("content_angle") != null ? item.get("content_angle").toString() : "Educational";
                                         
                    String postText = item.get("postText") != null ? item.get("postText").toString() : 
                                      item.get("post_text") != null ? item.get("post_text").toString() : "";
                    
                    // Append the required subscription link since we removed it from the LLM prompt to save tokens
                    if (!postText.contains("campaign.sailortoday.in")) {
                        postText = postText.trim() + "\n\nSubscribe: https://campaign.sailortoday.in/campaign?utmMedium=whatsapp";
                    }

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
