package com.marketingagent.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketingagent.domain.magazine.Magazine;
import com.marketingagent.domain.magazine.MagazineStatus;
import com.marketingagent.domain.magazine.Story;
import com.marketingagent.repository.MagazineRepository;
import com.marketingagent.repository.StoryRepository;
import com.marketingagent.webclient.groq.GroqClient;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class StoryExtractionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StoryExtractionService.class);

    private final MagazineRepository magazineRepository;
    private final StoryRepository storyRepository;
    private final GroqClient groqClient;
    private final ObjectMapper objectMapper;

    public StoryExtractionService(MagazineRepository magazineRepository, StoryRepository storyRepository, GroqClient groqClient, ObjectMapper objectMapper) {
        this.magazineRepository = magazineRepository;
        this.storyRepository = storyRepository;
        this.groqClient = groqClient;
        this.objectMapper = objectMapper;
    }

    @Async
    @Transactional
    public void extractStoriesAsync(UUID magazineId) {
        LOGGER.info("Starting async story extraction for magazine: {}", magazineId);
        
        Magazine magazine = magazineRepository.findById(magazineId).orElse(null);
        if (magazine == null) {
            LOGGER.error("Magazine not found for extraction: {}", magazineId);
            return;
        }

        try {
            magazine.setProcessingStatus(MagazineStatus.EXTRACTING);
            magazineRepository.save(magazine);

            // Step 1: Extract raw text from PDF
            String rawText = extractTextFromPdf(magazine.getFilePath());
            magazine.setExtractedText(rawText);
            
            // Limit text size to avoid Groq token limits (e.g. max 50k chars for ~15k tokens)
            String textForPrompt = rawText.length() > 50000 ? rawText.substring(0, 50000) : rawText;

            // Step 2: Extract structured stories using Groq
            String systemPrompt = "You are an expert marketing AI. Your task is to extract individual stories, articles, or coherent sections from the provided magazine text. " +
                    "Return ONLY a JSON array of objects. Do not wrap it in markdown. Each object must have: " +
                    "'title' (string), 'summary' (string), 'keywords' (array of strings), 'contentAngle' (string, e.g. Educational, Inspirational, Promotional).";
            
            String userPrompt = "Extract stories from the following text:\\n\\n" + textForPrompt;

            String jsonResponse = groqClient.generateCompletion(systemPrompt, userPrompt);
            
            // Clean markdown blocks if Groq returns them
            jsonResponse = jsonResponse.replaceAll("```json\\\\s*", "").replaceAll("```\\\\s*", "").trim();

            List<Map<String, Object>> extractedItems = objectMapper.readValue(jsonResponse, new TypeReference<>() {});

            for (Map<String, Object> item : extractedItems) {
                Story story = new Story(magazine.getTenant(), magazine, (String) item.get("title"));
                story.setSummary((String) item.get("summary"));
                story.setKeywords((List<String>) item.get("keywords"));
                story.setContentAngle((String) item.get("contentAngle"));
                storyRepository.save(story);
            }

            magazine.setProcessingStatus(MagazineStatus.PROCESSED);
            magazineRepository.save(magazine);
            LOGGER.info("Successfully extracted {} stories for magazine {}", extractedItems.size(), magazineId);

        } catch (Exception e) {
            LOGGER.error("Failed to extract stories for magazine {}", magazineId, e);
            magazine.setProcessingStatus(MagazineStatus.FAILED);
            magazine.setErrorMessage(e.getMessage());
            magazineRepository.save(magazine);
        }
    }

    private String extractTextFromPdf(String filePath) throws Exception {
        File pdfFile = null;
        boolean isTempFile = false;
        try {
            if (filePath.startsWith("http")) {
                File tempFile = File.createTempFile("extract-", ".pdf");
                pdfFile = tempFile;
                isTempFile = true;
                org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
                restTemplate.execute(filePath, org.springframework.http.HttpMethod.GET, null, clientHttpResponse -> {
                    java.nio.file.Files.copy(clientHttpResponse.getBody(), tempFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    return null;
                });
            } else {
                pdfFile = new File(filePath);
            }

            try (PDDocument document = org.apache.pdfbox.Loader.loadPDF(pdfFile)) {
                PDFTextStripper stripper = new PDFTextStripper();
                return stripper.getText(document);
            }
        } finally {
            if (isTempFile && pdfFile != null && pdfFile.exists()) {
                pdfFile.delete();
            }
        }
    }
}
