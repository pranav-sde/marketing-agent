package com.marketingagent.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PdfProcessingServiceTest {

    private PDFProcessingService pdfProcessingService;
    private StorageService storageService;

    @BeforeEach
    public void setUp() {
        storageService = new StorageService();
        // Force initialization for unit testing
        try {
            java.lang.reflect.Field uploadDirField = StorageService.class.getDeclaredField("uploadDirStr");
            uploadDirField.setAccessible(true);
            uploadDirField.set(storageService, "/tmp/marketing-agent-test-uploads");
            
            java.lang.reflect.Field portField = StorageService.class.getDeclaredField("serverPort");
            portField.setAccessible(true);
            portField.set(storageService, 8080);
        } catch (Exception e) {
            e.printStackTrace();
        }
        storageService.init();

        pdfProcessingService = new PDFProcessingService();
        try {
            java.lang.reflect.Field storageField = PDFProcessingService.class.getDeclaredField("storageService");
            storageField.setAccessible(true);
            storageField.set(pdfProcessingService, storageService);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testCreateAndExtractPdf(@TempDir Path tempDir) throws Exception {
        Path pdfPath = tempDir.resolve("test-magazine.pdf");
        
        // 1. Create a dummy PDF with text using PDFBox 3.x
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
                contentStream.newLineAtOffset(100, 700);
                contentStream.showText("SailorToday Magazine - March 2026 Edition");
                contentStream.endText();
            }
            document.save(new File(pdfPath.toUri()));
        }

        // 2. Test text extraction
        String extractedText = pdfProcessingService.extractText(pdfPath);
        assertTrue(extractedText.contains("SailorToday Magazine"));

        // 3. Test image/page rendering extraction
        List<String> images = pdfProcessingService.extractImages(pdfPath);
        assertFalse(images.isEmpty(), "Images list should not be empty (should contain cover render)");
        
        // Cover page should be first
        String coverImg = images.get(0);
        assertNotNull(coverImg);
        
        Path coverImgPath = storageService.load(coverImg);
        assertTrue(coverImgPath.toFile().exists(), "Cover image file should exist on disk");
    }
}
