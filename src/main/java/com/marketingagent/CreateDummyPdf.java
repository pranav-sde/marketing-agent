package com.marketingagent;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import java.io.File;

public class CreateDummyPdf {
    public static void main(String[] args) throws Exception {
        try (PDDocument document = new PDDocument()) {
            for (int i = 1; i <= 3; i++) {
                PDPage page = new PDPage();
                document.addPage(page);
                try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                    contentStream.beginText();
                    contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
                    contentStream.newLineAtOffset(100, 700);
                    contentStream.showText("SailorToday Magazine - Page " + i + " Content");
                    contentStream.newLineAtOffset(0, -20);
                    contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                    contentStream.showText("This page contains deep maritime insights, global safety protocols, and industry news.");
                    contentStream.endText();
                }
            }
            document.save(new File("/Users/pranav/Documents/Marketing Agent/dummy-magazine.pdf"));
            System.out.println("Dummy PDF generated successfully at /Users/pranav/Documents/Marketing Agent/dummy-magazine.pdf");
        }
    }
}
