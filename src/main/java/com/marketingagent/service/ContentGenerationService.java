package com.marketingagent.service;

import com.marketingagent.domain.magazine.ContentCalendar;
import com.marketingagent.domain.magazine.ContentCalendarStatus;
import com.marketingagent.domain.magazine.ContentPlatform;
import com.marketingagent.domain.magazine.GeneratedContent;
import com.marketingagent.repository.ContentCalendarRepository;
import com.marketingagent.repository.GeneratedContentRepository;
import com.marketingagent.webclient.groq.GroqClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ContentGenerationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContentGenerationService.class);

    private final ContentCalendarRepository contentCalendarRepository;
    private final GeneratedContentRepository generatedContentRepository;
    private final GroqClient groqClient;

    public ContentGenerationService(
            ContentCalendarRepository contentCalendarRepository,
            GeneratedContentRepository generatedContentRepository,
            GroqClient groqClient) {
        this.contentCalendarRepository = contentCalendarRepository;
        this.generatedContentRepository = generatedContentRepository;
        this.groqClient = groqClient;
    }

    @Async
    @Transactional
    public void generateContentForCalendarAsync(UUID magazineId) {
        LOGGER.info("Starting async content generation for 30-day calendar of magazine: {}", magazineId);
        List<ContentCalendar> entries = contentCalendarRepository.findByMagazine_IdOrderByDayNumberAsc(magazineId);

        for (ContentCalendar entry : entries) {
            try {
                // For MVP, we only generate WhatsApp. Can loop over ContentPlatform.values() in future.
                generatePlatformContent(entry, ContentPlatform.WHATSAPP);
                
                entry.setStatus(ContentCalendarStatus.GENERATED);
                contentCalendarRepository.save(entry);
                
            } catch (Exception e) {
                LOGGER.error("Failed to generate content for calendar entry {}", entry.getId(), e);
                entry.setStatus(ContentCalendarStatus.FAILED);
                contentCalendarRepository.save(entry);
            }
        }
    }

    private void generatePlatformContent(ContentCalendar entry, ContentPlatform platform) {
        // Skip if already generated
        if (generatedContentRepository.findByCalendarEntry_IdAndPlatform(entry.getId(), platform).isPresent()) {
            return;
        }

        String platformName = platform.name().toLowerCase();
        String ctaLink = "https://campaign.sailortoday.in/campaign?utmMedium=" + platformName;

        String systemPrompt = "You are an expert social media copywriter. You write engaging, highly converting short posts.";
        String userPrompt = String.format("""
                Create a %s marketing post based on the following story summary.
                Story Title: %s
                Summary: %s
                Keywords: %s
                Content Angle: %s
                
                Rules:
                1. Length must be strictly between 150-200 characters.
                2. Include 2-3 relevant emojis.
                3. End with the exact Call to Action: "Subscribe: %s"
                4. Include 2-3 hashtags.
                5. Return ONLY the final message text, no explanations, no markdown blocks.
                """,
                platform.name(),
                entry.getStory().getTitle(),
                entry.getStory().getSummary(),
                String.join(", ", entry.getStory().getKeywords() != null ? entry.getStory().getKeywords() : List.of()),
                entry.getContentAngle(),
                ctaLink
        );

        String generatedMessage = groqClient.generateCompletion(systemPrompt, userPrompt);
        
        GeneratedContent content = new GeneratedContent(
                entry.getTenant(),
                entry,
                generatedMessage.trim(),
                platform
        );
        
        // Example parsing hashtags, can be refined based on actual Llama output
        content.setHashtags(List.of("Marketing", "Agent")); 

        generatedContentRepository.save(content);
        LOGGER.debug("Generated {} content for calendar entry day {}", platform.name(), entry.getDayNumber());
    }
}
