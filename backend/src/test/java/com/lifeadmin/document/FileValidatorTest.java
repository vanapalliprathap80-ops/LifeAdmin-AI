package com.lifeadmin.document;

import com.lifeadmin.extraction.ExtractionException;
import com.lifeadmin.extraction.PdfExtractionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class FileValidatorTest {

    @Mock
    private PdfExtractionService pdfExtractionService;

    private FileValidator fileValidator;

    @BeforeEach
    void setUp() {
        fileValidator = new FileValidator(pdfExtractionService);
    }

    // ── valid ────────────────────────────────────────────────────────────────

    @Test
    void valid_pdf_passes_validation() {
        doNothing().when(pdfExtractionService).validatePdf(any(InputStream.class));
        MockMultipartFile file = pdf("document.pdf", 1000);

        assertThatNoException().isThrownBy(() -> fileValidator.validate(file));
    }

    // ── empty ────────────────────────────────────────────────────────────────

    @Test
    void empty_file_is_rejected() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> fileValidator.validate(file))
                .isInstanceOf(InvalidDocumentException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void null_file_is_rejected() {
        assertThatThrownBy(() -> fileValidator.validate(null))
                .isInstanceOf(InvalidDocumentException.class);
    }

    // ── wrong extension ───────────────────────────────────────────────────────

    @Test
    void non_pdf_extension_is_rejected() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "malware.exe", "application/pdf", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> fileValidator.validate(file))
                .isInstanceOf(InvalidDocumentException.class)
                .hasMessageContaining("PDF");
    }

    @Test
    void txt_extension_is_rejected() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "notes.txt", "text/plain", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> fileValidator.validate(file))
                .isInstanceOf(InvalidDocumentException.class);
    }

    // ── wrong content-type ────────────────────────────────────────────────────

    @Test
    void wrong_content_type_is_rejected() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "document.pdf", "image/jpeg", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> fileValidator.validate(file))
                .isInstanceOf(InvalidDocumentException.class);
    }

    // ── path traversal ────────────────────────────────────────────────────────

    @Test
    void path_traversal_filename_is_rejected() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "../../etc/passwd.pdf", "application/pdf", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> fileValidator.validate(file))
                .isInstanceOf(InvalidDocumentException.class);
    }

    @Test
    void absolute_path_filename_is_rejected() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "/etc/shadow.pdf", "application/pdf", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> fileValidator.validate(file))
                .isInstanceOf(InvalidDocumentException.class);
    }

    @Test
    void windows_path_traversal_is_rejected() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "..\\..\\windows\\system32.pdf", "application/pdf", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> fileValidator.validate(file))
                .isInstanceOf(InvalidDocumentException.class);
    }

    // ── invalid PDF ───────────────────────────────────────────────────────────

    @Test
    void non_parseable_pdf_is_rejected() {
        doThrow(new ExtractionException("Not a valid PDF"))
                .when(pdfExtractionService).validatePdf(any(InputStream.class));

        MockMultipartFile file = pdf("fake.pdf", 1000);

        assertThatThrownBy(() -> fileValidator.validate(file))
                .isInstanceOf(InvalidDocumentException.class)
                .hasMessageContaining("valid PDF");
    }

    // ── oversized ─────────────────────────────────────────────────────────────

    @Test
    void oversized_file_is_rejected() {
        // Create a mock that reports a size > 50 MB without actually allocating it
        MockMultipartFile file = new MockMultipartFile(
                "file", "large.pdf", "application/pdf",
                new byte[]{1, 2, 3}) {
            @Override
            public long getSize() {
                return 51L * 1024 * 1024; // 51 MB
            }
        };

        assertThatThrownBy(() -> fileValidator.validate(file))
                .isInstanceOf(InvalidDocumentException.class)
                .hasMessageContaining("50 MB");
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private MockMultipartFile pdf(String name, int size) {
        byte[] content = new byte[size];
        content[0] = '%'; content[1] = 'P'; content[2] = 'D'; content[3] = 'F';
        return new MockMultipartFile("file", name, "application/pdf", content);
    }
}
