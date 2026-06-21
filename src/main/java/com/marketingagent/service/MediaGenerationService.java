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
     * Renders page 0 of the magazine PDF as a PNG cover and uploads it to storage.
     */
    public String generateMagazineCover(Magazine magazine) {
        String filePath = magazine.getFilePath();
        if (filePath == null || filePath.isBlank()) {
            LOGGER.warn("No magazine file path found. Using stock cover fallback.");
            return getStockFallbackUrl("shipping,marine");
        }

        File pdfFile = null;
        boolean isTempFile = false;
        try {
            if (filePath.startsWith("http")) {
                isTempFile = true;
            }
            pdfFile = getLocalFile(filePath);
        } catch (Exception e) {
            LOGGER.error("Failed to download or read magazine PDF at {}. Using stock cover fallback.", filePath, e);
            return getStockFallbackUrl("shipping,marine");
        }

        try {
            if (!pdfFile.exists()) {
                LOGGER.warn("Magazine PDF file not found at path: {}. Using stock cover fallback.", filePath);
                return getStockFallbackUrl("shipping,marine");
            }

            try (PDDocument document = Loader.loadPDF(pdfFile)) {
                if (document.getNumberOfPages() == 0) {
                    LOGGER.warn("Magazine PDF is empty. Using stock cover fallback.");
                    return getStockFallbackUrl("shipping,marine");
                }

                LOGGER.info("Rendering PDF cover page for magazine: {}", magazine.getTitle());
                PDFRenderer pdfRenderer = new PDFRenderer(document);
                // Render first page (0) at 72 DPI to save memory
                BufferedImage bim = pdfRenderer.renderImageWithDPI(0, 72);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(bim, "png", baos);
                byte[] imageBytes = baos.toByteArray();
                
                bim.flush();

                String key = "magazine-cover-" + magazine.getId() + ".png";
                return storageService.uploadFile(key, imageBytes, "image/png");

            } catch (Exception e) {
                LOGGER.error("Failed to render magazine PDF cover. Falling back to stock cover.", e);
                return getStockFallbackUrl("shipping,marine");
            }
        } finally {
            if (isTempFile && pdfFile != null && pdfFile.exists()) {
                pdfFile.delete();
            }
        }
    }

    public String generateContentMedia(ContentCalendar entry) {
        Magazine magazine = entry.getMagazine();
        if (magazine != null) {
            String filePath = magazine.getFilePath();
            if (filePath != null && !filePath.isBlank()) {
                File magFile = null;
                boolean isTempFile = false;
                try {
                    if (filePath.startsWith("http")) {
                        isTempFile = true;
                    }
                    magFile = getLocalFile(filePath);
                    if (magFile.exists()) {
                        String mimeType = magazine.getMimeType();
                        if (mimeType != null && mimeType.toLowerCase().contains("pdf")) {
                            return generateMediaFromPdf(entry, magFile);
                        } else if (mimeType != null && mimeType.toLowerCase().contains("image")) {
                            return generateMediaFromImage(entry, magFile);
                        } else if (filePath.toLowerCase().endsWith(".pdf")) {
                            return generateMediaFromPdf(entry, magFile);
                        } else if (filePath.toLowerCase().matches(".*\\.(jpg|jpeg|png)$")) {
                            return generateMediaFromImage(entry, magFile);
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to generate media from magazine file. Using fallback.", e);
                } finally {
                    if (isTempFile && magFile != null && magFile.exists()) {
                        magFile.delete();
                    }
                }
            }
        }

        // Fallback to loremflickr if magazine file is not usable
        return generateStockImage(entry);
    }

    private File getLocalFile(String path) throws Exception {
        if (path.startsWith("http")) {
            File tempFile = File.createTempFile("mag-dl-", path.toLowerCase().endsWith(".pdf") ? ".pdf" : ".tmp");
            restTemplate.execute(path, org.springframework.http.HttpMethod.GET, null, clientHttpResponse -> {
                java.nio.file.Files.copy(clientHttpResponse.getBody(), tempFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                return null;
            });
            return tempFile;
        }
        return new File(path);
    }

    private final java.util.Map<UUID, List<Integer>> bestPagesCache = new java.util.concurrent.ConcurrentHashMap<>();

    private List<Integer> getBestImagePages(PDDocument document, UUID magazineId) throws java.io.IOException {
        if (bestPagesCache.containsKey(magazineId)) {
            return bestPagesCache.get(magazineId);
        }
        
        int totalPages = document.getNumberOfPages();
        List<PageScore> scores = new java.util.ArrayList<>();
        
        // Skip cover (0) and last page (totalPages - 1)
        int startPage = 1;
        int endPage = totalPages - 2;
        
        if (endPage < startPage) {
            return List.of(0); // fallback
        }
        
        org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();
        for (int i = startPage; i <= endPage; i++) {
            stripper.setStartPage(i + 1); // 1-based
            stripper.setEndPage(i + 1);
            String text = stripper.getText(document);
            scores.add(new PageScore(i, text != null ? text.trim().length() : 0));
        }
        
        // Sort by text length ascending (less text = more likely to be an image page)
        scores.sort((s1, s2) -> {
            int cmp = Integer.compare(s1.textLength, s2.textLength);
            if (cmp == 0) return Integer.compare(s1.pageIndex, s2.pageIndex);
            return cmp;
        });
        
        List<Integer> bestPages = scores.stream().map(s -> s.pageIndex).toList();
        bestPagesCache.put(magazineId, bestPages);
        return bestPages;
    }
    
    private static class PageScore {
        int pageIndex;
        int textLength;
        PageScore(int pageIndex, int textLength) {
            this.pageIndex = pageIndex;
            this.textLength = textLength;
        }
    }

    private String generateMediaFromPdf(ContentCalendar entry, File pdfFile) throws Exception {
        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            int totalPages = document.getNumberOfPages();
            if (totalPages == 0) {
                return generateStockImage(entry);
            }

            List<Integer> bestPages = getBestImagePages(document, entry.getMagazine().getId());
            
            // Post index: Day 2 -> 0, Day 3 -> 1, ..., Day 30 -> 28
            int postIndex = entry.getDayNumber() - 2;
            if (postIndex < 0) postIndex = 0;
            
            int selectedPageIndex = bestPages.get(postIndex % bestPages.size());

            String key = "magazines/" + entry.getMagazine().getId() + "/pages/page-" + selectedPageIndex + ".png";

            if (storageService.fileExists(key)) {
                LOGGER.info("PDF page {} already rendered for magazine {}. Reusing existing S3 image.", selectedPageIndex, entry.getMagazine().getId());
                return storageService.getFileUrl(key);
            }

            LOGGER.info("Rendering PDF page {} for content calendar day {}", selectedPageIndex, entry.getDayNumber());
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            // Render at 72 DPI to save memory in Render's 512MB limit
            BufferedImage bim = pdfRenderer.renderImageWithDPI(selectedPageIndex, 72);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bim, "png", baos);
            byte[] imageBytes = baos.toByteArray();
            
            bim.flush();

            return storageService.uploadFile(key, imageBytes, "image/png");
        }
    }

    private String generateMediaFromImage(ContentCalendar entry, File imageFile) throws Exception {
        String extension = "jpg";
        if (imageFile.getName().toLowerCase().endsWith(".png")) {
            extension = "png";
        }
        
        String key = "magazines/" + entry.getMagazine().getId() + "/cover." + extension;
        
        if (storageService.fileExists(key)) {
            LOGGER.info("Image already uploaded for magazine {}. Reusing existing S3 image.", entry.getMagazine().getId());
            return storageService.getFileUrl(key);
        }

        LOGGER.info("Using uploaded image file directly for content media");
        byte[] imageBytes = java.nio.file.Files.readAllBytes(imageFile.toPath());
        String contentType = extension.equals("png") ? "image/png" : "image/jpeg";
        return storageService.uploadFile(key, imageBytes, contentType);
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
        // Direct loremflickr fallback url (if download fails, we just return the direct link)
        return "https://loremflickr.com/800/600/" + query;
    }
}
