package com.marketingagent.service;

import com.marketingagent.exception.PdfProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

public class PdfProcessingServiceTest {

    private PdfProcessingService pdfProcessingService;

    @BeforeEach
    public void setUp() {
        pdfProcessingService = new PdfProcessingService();
    }

    @Test
    public void testExtractTextWithNullInputStream() {
        PdfProcessingException exception = assertThrows(PdfProcessingException.class, () -> {
            pdfProcessingService.extractText(null);
        });
        assertTrue(exception.getMessage().contains("InputStream is null"));
    }

    @Test
    public void testExtractTextWithEmptyInputStream() {
        InputStream emptyStream = new ByteArrayInputStream(new byte[0]);
        PdfProcessingException exception = assertThrows(PdfProcessingException.class, () -> {
            pdfProcessingService.extractText(emptyStream);
        });
        assertTrue(exception.getMessage().contains("Corrupted PDF") || exception.getMessage().contains("Failed to parse"));
    }

    @Test
    public void testExtractTextWithCorruptedBytes() {
        byte[] corruptedBytes = "This is not a valid PDF file structure at all".getBytes();
        InputStream corruptedStream = new ByteArrayInputStream(corruptedBytes);
        
        PdfProcessingException exception = assertThrows(PdfProcessingException.class, () -> {
            pdfProcessingService.extractText(corruptedStream);
        });
        assertTrue(exception.getMessage().contains("Corrupted PDF"));
    }
}
