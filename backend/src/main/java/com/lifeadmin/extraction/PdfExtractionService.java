package com.lifeadmin.extraction;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

/**
 * Responsible ONLY for extracting text from a PDF byte stream.
 * Does not perform AI analysis, obligation extraction, or any interpretation.
 *
 * Behaviour for image-only/scanned PDFs:
 *   PDFBox will return an empty or near-empty string.
 *   The caller is responsible for deciding how to handle blank extraction
 *   (Phase 3 marks such documents FAILED; Phase 4+ may introduce OCR).
 */
@Service
public class PdfExtractionService {

    private static final Logger log = LoggerFactory.getLogger(PdfExtractionService.class);

    /**
     * Minimum characters required to consider extraction successful.
     * Image-only PDFs typically yield 0–5 characters of whitespace.
     */
    private static final int MIN_EXTRACTABLE_CHARS = 10;

    /**
     * Validates that the input stream contains a parseable PDF.
     * Closes the stream after reading.
     *
     * @throws ExtractionException if the stream is not a valid PDF
     */
    public void validatePdf(InputStream inputStream) {
        try (PDDocument doc = Loader.loadPDF(inputStream.readAllBytes())) {
            if (doc.getNumberOfPages() == 0) {
                throw new ExtractionException("PDF contains no pages");
            }
        } catch (IOException e) {
            throw new ExtractionException("File is not a valid or parseable PDF", e);
        }
    }

    /**
     * Extracts all text from the PDF at the given path.
     *
     * @param pdfBytes raw bytes of the PDF file
     * @return the extracted text (may be empty for image-only PDFs)
     * @throws ExtractionException if the PDF cannot be loaded or parsed
     */
    public String extract(byte[] pdfBytes) {
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);
            log.info("Extracted {} characters from PDF ({} pages)", text.length(), doc.getNumberOfPages());
            return text;
        } catch (IOException e) {
            throw new ExtractionException("Failed to extract text from PDF", e);
        }
    }

    /**
     * Returns true if the extracted text meets the minimum threshold for a
     * text-based PDF. Image-only PDFs will fail this check.
     */
    public boolean hasExtractableText(String extractedText) {
        return extractedText != null && extractedText.strip().length() >= MIN_EXTRACTABLE_CHARS;
    }
}
