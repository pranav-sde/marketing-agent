package com.marketingagent.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PDFProcessingService {

    @Autowired
    private StorageService storageService;

    public String extractText(Path pdfPath) {
        try (PDDocument document = Loader.loadPDF(new RandomAccessReadBufferedFile(new File(pdfPath.toUri())))) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (IOException e) {
            throw new RuntimeException("Failed to extract text from PDF: " + pdfPath.getFileName(), e);
        }
    }

    public List<String> extractImages(Path pdfPath) {
        List<String> imageFilenames = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(new RandomAccessReadBufferedFile(new File(pdfPath.toUri())))) {
            PDFRenderer renderer = new PDFRenderer(document);

            // 1. Render Cover Page (Page 0)
            if (document.getNumberOfPages() > 0) {
                try {
                    BufferedImage coverBim = renderer.renderImageWithDPI(0, 150);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(coverBim, "png", baos);
                    String coverName = storageService.storeFile(baos.toByteArray(), "cover.png");
                    imageFilenames.add(coverName); // The first item will be the cover page image
                } catch (Exception ex) {
                    System.err.println("Could not render cover page: " + ex.getMessage());
                }
            }

            // 2. Extract embedded images from pages
            int imageCount = 0;
            for (int pageNum = 0; pageNum < document.getNumberOfPages(); pageNum++) {
                PDPage page = document.getPage(pageNum);
                PDResources resources = page.getResources();
                if (resources == null) continue;

                for (COSName name : resources.getXObjectNames()) {
                    try {
                        PDXObject xobject = resources.getXObject(name);
                        if (xobject instanceof PDImageXObject) {
                            PDImageXObject image = (PDImageXObject) xobject;
                            BufferedImage bim = image.getImage();
                            
                            // Filter out small icons/lines and blank images
                            if (bim.getWidth() >= 150 && bim.getHeight() >= 150 && !isImageBlank(bim)) {
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                ImageIO.write(bim, "png", baos);
                                String imgName = storageService.storeFile(baos.toByteArray(), "extracted_img_" + imageCount + ".png");
                                imageFilenames.add(imgName);
                                imageCount++;
                            }
                        }
                    } catch (Exception ex) {
                        System.err.println("Could not extract image " + name + " on page " + pageNum + ": " + ex.getMessage());
                    }
                }
            }

            // 3. Fallback: If we have fewer than 30 images in total (including cover),
            // render entire pages as high-quality PNGs to use as content visuals.
            int pagesToRender = Math.min(document.getNumberOfPages(), 30);
            for (int pageNum = 1; pageNum < pagesToRender && imageFilenames.size() < 30; pageNum++) {
                try {
                    BufferedImage pageBim = renderer.renderImageWithDPI(pageNum, 120);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(pageBim, "png", baos);
                    String pageImgName = storageService.storeFile(baos.toByteArray(), "page_screenshot_" + pageNum + ".png");
                    imageFilenames.add(pageImgName);
                } catch (Exception ex) {
                    System.err.println("Could not render page " + pageNum + " as fallback: " + ex.getMessage());
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to process PDF assets: " + pdfPath.getFileName(), e);
        }

        return imageFilenames;
    }

    public String renderPageAsImage(Path pdfPath, int pageIndex) {
        try (PDDocument document = Loader.loadPDF(new RandomAccessReadBufferedFile(new File(pdfPath.toUri())))) {
            PDFRenderer renderer = new PDFRenderer(document);
            int totalPages = document.getNumberOfPages();
            if (totalPages == 0) {
                return null;
            }
            int targetPage = pageIndex;
            if (targetPage < 0) {
                targetPage = 0;
            }
            if (targetPage >= totalPages) {
                targetPage = targetPage % totalPages;
            }
            
            BufferedImage bim = renderer.renderImageWithDPI(targetPage, 150);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bim, "png", baos);
            return storageService.storeFile(baos.toByteArray(), "regenerated_page_" + targetPage + ".png");
        } catch (IOException e) {
            throw new RuntimeException("Failed to render page " + pageIndex + " as image: " + pdfPath.getFileName(), e);
        }
    }

    private boolean isImageBlank(BufferedImage bim) {
        int width = bim.getWidth();
        int height = bim.getHeight();
        
        // Sample pixels to detect solid color (e.g. transparent or white)
        int step = Math.max(1, Math.min(width, height) / 20);
        Map<Integer, Integer> colorCounts = new HashMap<>();
        int totalSampled = 0;
        
        for (int x = 0; x < width; x += step) {
            for (int y = 0; y < height; y += step) {
                int rgb = bim.getRGB(x, y);
                // Group fully transparent pixels together
                int alpha = (rgb >> 24) & 0xff;
                if (alpha == 0) {
                    rgb = 0x00000000;
                }
                colorCounts.put(rgb, colorCounts.getOrDefault(rgb, 0) + 1);
                totalSampled++;
            }
        }
        
        if (totalSampled == 0) return true;
        
        for (int count : colorCounts.values()) {
            // If more than 95% of pixels are the exact same color, it's blank/solid
            if ((double) count / totalSampled > 0.95) {
                return true;
            }
        }
        
        return false;
    }
}
