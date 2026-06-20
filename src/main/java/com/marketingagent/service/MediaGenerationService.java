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

        File pdfFile = new File(filePath);
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
            bim = null;

            String key = "magazine-cover-" + magazine.getId() + ".png";
            return storageService.uploadFile(key, imageBytes, "image/png");

        } catch (Exception e) {
            LOGGER.error("Failed to render magazine PDF cover. Falling back to stock cover.", e);
            return getStockFallbackUrl("shipping,marine");
        }
    }

    public String generateContentMedia(ContentCalendar entry) {
        Magazine magazine = entry.getMagazine();
        if (magazine != null) {
            String filePath = magazine.getFilePath();
            if (filePath != null && !filePath.isBlank()) {
                File magFile = new File(filePath);
                if (magFile.exists()) {
                    try {
                        String mimeType = magazine.getMimeType();
                        if (mimeType != null && mimeType.toLowerCase().contains("pdf")) {
                            return generateMediaFromPdf(entry, magFile);
                        } else if (mimeType != null && mimeType.toLowerCase().contains("image")) {
                            return generateMediaFromImage(magFile);
                        } else if (filePath.toLowerCase().endsWith(".pdf")) {
                            return generateMediaFromPdf(entry, magFile);
                        } else if (filePath.toLowerCase().matches(".*\\.(jpg|jpeg|png)$")) {
                            return generateMediaFromImage(magFile);
                        }
                    } catch (Exception e) {
                        LOGGER.error("Failed to generate media from magazine file. Using fallback.", e);
                    }
                }
            }
        }

        // Fallback to loremflickr if magazine file is not usable
        return generateStockImage(entry);
    }

    private String generateMediaFromPdf(ContentCalendar entry, File pdfFile) throws Exception {
        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            int totalPages = document.getNumberOfPages();
            if (totalPages == 0) {
                return generateStockImage(entry);
            }

            // Pick a page based on day number to provide some variety
            int pageIndex = entry.getDayNumber() % totalPages;

            LOGGER.info("Rendering PDF page {} for content calendar day {}", pageIndex, entry.getDayNumber());
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            // Render at 72 DPI to save memory in Render's 512MB limit
            BufferedImage bim = pdfRenderer.renderImageWithDPI(pageIndex, 72);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bim, "png", baos);
            byte[] imageBytes = baos.toByteArray();
            
            bim.flush();
            bim = null;

            String key = "media-pdf-page-" + entry.getDayNumber() + "-" + UUID.randomUUID() + ".png";
            return storageService.uploadFile(key, imageBytes, "image/png");
        }
    }

    private String generateMediaFromImage(File imageFile) throws Exception {
        LOGGER.info("Using uploaded image file directly for content media");
        byte[] imageBytes = java.nio.file.Files.readAllBytes(imageFile.toPath());
        String extension = "jpg";
        if (imageFile.getName().toLowerCase().endsWith(".png")) {
            extension = "png";
        }
        String key = "media-mag-image-" + UUID.randomUUID() + "." + extension;
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
