package com.marketingagent.service;

import com.marketingagent.exception.PdfProcessingException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
public class PdfProcessingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdfProcessingService.class);

    /**
     * Extracts all text from a PDF InputStream.
     * Uses PDFBox 3.0.x with RandomAccessReadBuffer for streaming.
     */
    public String extractText(InputStream inputStream) {
        if (inputStream == null) {
            throw new PdfProcessingException("Invalid PDF stream: InputStream is null.");
        }

        LOGGER.info("Starting text extraction from PDF InputStream...");

        try (PDDocument document = Loader.loadPDF(new RandomAccessReadBuffer(inputStream))) {
            int pageCount = document.getNumberOfPages();
            LOGGER.info("Loaded PDF document successfully. Total pages: {}", pageCount);

            if (pageCount == 0) {
                throw new PdfProcessingException("Corrupted or invalid PDF: Document has 0 pages.");
            }

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            if (text == null || text.strip().isEmpty()) {
                throw new PdfProcessingException("Empty PDF content: No readable text could be extracted.");
            }

            LOGGER.info("Extracted {} characters of text from PDF.", text.length());
            return text;

        } catch (PdfProcessingException e) {
            // Re-throw our custom exception directly
            throw e;
        } catch (Exception e) {
            LOGGER.error("Failed to parse PDF document due to corruption or read failure", e);
            throw new PdfProcessingException("Corrupted PDF: Failed to parse PDF structure or read stream. " + e.getMessage(), e);
        }
    }
}
