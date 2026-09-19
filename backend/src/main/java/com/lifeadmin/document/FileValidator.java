package com.lifeadmin.document;

import com.lifeadmin.extraction.ExtractionException;
import com.lifeadmin.extraction.PdfExtractionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;

/**
 * Validates uploaded files before storage.
 *
 * Validation order:
 *  1. Not empty
 *  2. Size limit (50 MB)
 *  3. Extension is .pdf (case-insensitive)
 *  4. Content-type header contains "pdf" (informational, not sole authority)
 *  5. Filename safety (no path traversal characters)
 *  6. PDF structural validity via PDFBox
 *
 * The last check (PDFBox parse) is the authoritative file-type gate.
 * It rejects files that claim to be PDFs but are not parseable.
 */
@Component
public class FileValidator {

    private static final Logger log = LoggerFactory.getLogger(FileValidator.class);

    private static final long MAX_SIZE_BYTES = 50L * 1024 * 1024; // 50 MB
    private static final Set<String> DANGEROUS_PATTERNS = Set.of(
            "..", "/", "\\", "\0", ":", "*", "?", "\"", "<", ">", "|"
    );

    private final PdfExtractionService pdfExtractionService;

    public FileValidator(PdfExtractionService pdfExtractionService) {
        this.pdfExtractionService = pdfExtractionService;
    }

    /**
     * Performs all validation checks on the uploaded file.
     *
     * @throws InvalidDocumentException with a client-safe message on any failure
     */
    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidDocumentException("No file provided or file is empty");
        }

        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new InvalidDocumentException("File exceeds maximum allowed size of 50 MB");
        }

        String originalFilename = file.getOriginalFilename();
        validateFilename(originalFilename);
        validateExtension(originalFilename);
        validateContentType(file.getContentType());
        validatePdfStructure(file);
    }

    // ─── private ──────────────────────────────────────────────────────────────

    private void validateFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new InvalidDocumentException("Filename is missing");
        }
        for (String dangerous : DANGEROUS_PATTERNS) {
            if (filename.contains(dangerous)) {
                log.warn("Rejected filename with dangerous pattern '{}': {}", dangerous, sanitiseForLog(filename));
                throw new InvalidDocumentException("Invalid filename");
            }
        }
    }

    private void validateExtension(String filename) {
        if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
            throw new InvalidDocumentException("Only PDF files are accepted");
        }
    }

    private void validateContentType(String contentType) {
        if (contentType == null || !contentType.toLowerCase().contains("pdf")) {
            // Log for monitoring but give the same generic message
            log.warn("Suspicious content-type on upload: {}", contentType);
            throw new InvalidDocumentException("Only PDF files are accepted");
        }
    }

    private void validatePdfStructure(MultipartFile file) {
        try {
            pdfExtractionService.validatePdf(file.getInputStream());
        } catch (IOException e) {
            throw new InvalidDocumentException("Could not read uploaded file");
        } catch (ExtractionException e) {
            log.warn("PDF structure validation failed: {}", e.getMessage());
            throw new InvalidDocumentException("File is not a valid PDF");
        }
    }

    /** Strip the filename to at most 80 chars for safe log output. */
    private String sanitiseForLog(String filename) {
        if (filename == null) return "<null>";
        return filename.length() > 80 ? filename.substring(0, 80) + "..." : filename;
    }
}
