package com.marketingagent.service;

import com.marketingagent.domain.magazine.ContentCalendar;
import com.marketingagent.domain.magazine.Magazine;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.List;
import java.util.UUID;

@Service
public class MediaGenerationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MediaGenerationService.class);

    private final StorageService storageService;
    private final RestTemplate restTemplate;

    public MediaGenerationService(StorageService storageService) {
        this.storageService = storageService;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Preload all media required for a magazine (Cover + 30 thumbnails).
     * This is called during the initial extraction pipeline while the local PDF file still exists.
     */
    public void preloadMagazineMedia(Magazine magazine, File localFile) {
        if (localFile == null || !localFile.exists()) {
            LOGGER.warn("Local file missing. Cannot preload media for magazine: {}", magazine.getId());
            return;
        }

        try {
            if (localFile.getName().toLowerCase().endsWith(".pdf") || 
               (magazine.getMimeType() != null && magazine.getMimeType().toLowerCase().contains("pdf"))) {
                preloadFromPdf(magazine, localFile);
            } else {
                preloadFromImage(magazine, localFile);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to preload media for magazine: {}", magazine.getId(), e);
        }
    }

    private void preloadFromPdf(Magazine magazine, File pdfFile) throws Exception {
        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            int totalPages = document.getNumberOfPages();
            if (totalPages == 0) return;

            PDFRenderer pdfRenderer = new PDFRenderer(document);

            // 1. Generate Cover (Page 0)
            LOGGER.info("Preloading cover image for magazine: {}", magazine.getId());
            BufferedImage coverBim = pdfRenderer.renderImageWithDPI(0, 72);
            ByteArrayOutputStream coverBaos = new ByteArrayOutputStream();
            ImageIO.write(coverBim, "png", coverBaos);
            storageService.uploadFile("magazine-cover-" + magazine.getId() + ".png", coverBaos.toByteArray(), "image/png");
            coverBim.flush();

            // 2. Generate 30 thumbnails based on the best image pages
            List<Integer> bestPages = getBestImagePages(document);
            if (bestPages.isEmpty()) bestPages.add(0);

            for (int day = 1; day <= 30; day++) {
                int selectedPageIndex = bestPages.get((day - 1) % bestPages.size());
                String key = "magazines/" + magazine.getId() + "/thumbnails/thumb-" + day + ".png";

                LOGGER.info("Preloading PDF page {} for content calendar day {}", selectedPageIndex, day);
                BufferedImage bim = pdfRenderer.renderImageWithDPI(selectedPageIndex, 72);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(bim, "png", baos);
                storageService.uploadFile(key, baos.toByteArray(), "image/png");
                bim.flush();
            }
            LOGGER.info("Successfully preloaded all media for magazine: {}", magazine.getId());
        }
    }

    private void preloadFromImage(Magazine magazine, File imageFile) throws Exception {
        byte[] imageBytes = java.nio.file.Files.readAllBytes(imageFile.toPath());
        
        // Cover
        storageService.uploadFile("magazine-cover-" + magazine.getId() + ".png", imageBytes, "image/png");

        // Thumbnails
        for (int day = 1; day <= 30; day++) {
            String key = "magazines/" + magazine.getId() + "/thumbnails/thumb-" + day + ".png";
            storageService.uploadFile(key, imageBytes, "image/png");
        }
    }

    private List<Integer> getBestImagePages(PDDocument document) throws java.io.IOException {
        int totalPages = document.getNumberOfPages();
        List<PageScore> scores = new java.util.ArrayList<>();
        int startPage = 1;
        int endPage = totalPages - 2;
        if (endPage < startPage) {
            return new java.util.ArrayList<>(List.of(0));
        }
        
        org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();
        for (int i = startPage; i <= endPage; i++) {
            stripper.setStartPage(i + 1); // 1-based
            stripper.setEndPage(i + 1);
            String text = stripper.getText(document);
            scores.add(new PageScore(i, text != null ? text.trim().length() : 0));
        }
        
        scores.sort((s1, s2) -> {
            int cmp = Integer.compare(s1.textLength, s2.textLength);
            if (cmp == 0) return Integer.compare(s1.pageIndex, s2.pageIndex);
            return cmp;
        });
        
        return scores.stream().map(s -> s.pageIndex).toList();
    }

    private static class PageScore {
        int pageIndex;
        int textLength;
        PageScore(int pageIndex, int textLength) {
            this.pageIndex = pageIndex;
            this.textLength = textLength;
        }
    }

    public String generateMagazineCover(Magazine magazine) {
        String key = "magazine-cover-" + magazine.getId() + ".png";
        return storageService.getFileUrl(key);
    }

    public String generateContentMedia(ContentCalendar entry) {
        if (entry.getMagazine() != null) {
            String key = "magazines/" + entry.getMagazine().getId() + "/thumbnails/thumb-" + entry.getDayNumber() + ".png";
            return storageService.getFileUrl(key);
        }
        return generateStockImage(entry);
    }

    private String generateStockImage(ContentCalendar entry) {
        List<String> keywords = entry.getStory().getKeywords();
        String query = "maritime,cargo"; // default query

        if (keywords != null && !keywords.isEmpty()) {
            query = String.join(",", keywords.stream().limit(2).toList())
                    .toLowerCase()
                    .replaceAll("\\s+", "");
        }

        String targetUrl = "https://loremflickr.com/800/600/" + query;
        LOGGER.info("Fetching fallback stock image from: {}", targetUrl);

        try {
            byte[] imageBytes = restTemplate.getForObject(targetUrl, byte[].class);
            if (imageBytes != null && imageBytes.length > 0) {
                String key = "media-image-" + UUID.randomUUID() + ".jpg";
                return storageService.uploadFile(key, imageBytes, "image/jpeg");
            }
        } catch (Exception e) {
            LOGGER.error("Failed to fetch stock fallback image. Returning raw URL.", e);
        }

        return getStockFallbackUrl(query);
    }

    private String getStockFallbackUrl(String query) {
        return "https://loremflickr.com/800/600/" + query;
    }
}
