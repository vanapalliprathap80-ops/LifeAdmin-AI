package com.lifeadmin;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * Standalone main class to generate a test PDF.
 * Usage: java -cp <classpath> com.lifeadmin.GenerateTestPdf <output-path>
 */
public class GenerateTestPdf {
    public static void main(String[] args) throws Exception {
        String outputPath = args.length > 0 ? args[0] : "test_output.pdf";

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.newLineAtOffset(50, 700);
                cs.showText("This is a lease agreement for 123 Main Street.");
                cs.newLineAtOffset(0, -20);
                cs.showText("Monthly rent is 1500 dollars.");
                cs.newLineAtOffset(0, -20);
                cs.showText("Renewal date: December 31 2026.");
                cs.endText();
            }
            try (OutputStream os = new FileOutputStream(outputPath)) {
                doc.save(os);
            }
        }
        System.out.println("PDF created: " + outputPath);
    }
}
