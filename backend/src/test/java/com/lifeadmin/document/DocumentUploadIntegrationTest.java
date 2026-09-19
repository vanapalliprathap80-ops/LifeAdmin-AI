package com.lifeadmin.document;

import com.lifeadmin.TestPdfFactory;
import com.lifeadmin.extraction.DocumentTextRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the document upload API.
 * Uses the real PostgreSQL database (same as Phase 2 tests).
 * @Transactional is NOT used here so we can verify real DB state
 * and test status transitions; cleanup is done manually.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "lifeadmin.storage.upload-dir=${java.io.tmpdir}/lifeadmin-test-uploads")
class DocumentUploadIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired DocumentRepository documentRepository;
    @Autowired DocumentTextRepository documentTextRepository;

    @org.springframework.boot.test.mock.mockito.MockBean
    com.lifeadmin.ai.AiAnalysisService aiAnalysisService;

    // ── Happy path: valid PDF ─────────────────────────────────────────────────

    @Test
    void upload_valid_pdf_returns_201_and_persists_document() throws Exception {
        byte[] pdfBytes = TestPdfFactory.create(
                "This is a lease agreement for property at 123 Main Street. " +
                "Monthly rent is $1500 due on the 1st of each month.");

        MockMultipartFile file = new MockMultipartFile(
                "file", "lease.pdf", "application/pdf", pdfBytes);

        MvcResult result = mockMvc.perform(multipart("/api/documents").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.originalFilename").value("lease.pdf"))
                .andExpect(jsonPath("$.data.processingStatus").value("COMPLETED"))
                .andReturn();

        // Verify DB state
        String body = result.getResponse().getContentAsString();
        String idStr = extractId(body);
        UUID id = UUID.fromString(idStr);

        Document doc = documentRepository.findById(id).orElseThrow();
        assertThat(doc.getProcessingStatus()).isEqualTo(ProcessingStatus.COMPLETED);
        assertThat(doc.getStoredFilename()).endsWith(".pdf");
        assertThat(doc.getStoredFilename()).doesNotContain("lease"); // not the user filename

        // Verify DocumentText was persisted
        var docText = documentTextRepository.findByDocumentId(id);
        assertThat(docText).isPresent();
        assertThat(docText.get().getExtractedText()).isNotBlank();
        assertThat(docText.get().getExtractedText()).contains("lease agreement");

        // Cleanup
        documentTextRepository.deleteById(docText.get().getId());
        documentRepository.deleteById(id);
    }

    // ── GET list and GET by ID ────────────────────────────────────────────────

    @Test
    void get_all_documents_returns_200() throws Exception {
        mockMvc.perform(get("/api/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void get_document_by_unknown_id_returns_404() throws Exception {
        mockMvc.perform(get("/api/documents/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ── Validation failures ───────────────────────────────────────────────────

    @Test
    void upload_empty_file_returns_422() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.pdf", "application/pdf", new byte[0]);

        mockMvc.perform(multipart("/api/documents").file(file))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void upload_non_pdf_extension_returns_422() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "document.docx", "application/pdf", new byte[]{1, 2, 3, 4});

        mockMvc.perform(multipart("/api/documents").file(file))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void upload_fake_pdf_bytes_returns_422() throws Exception {
        // File claims to be PDF but contains invalid bytes
        byte[] fakeContent = "This is definitely not a real PDF file.".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file", "fake.pdf", "application/pdf", fakeContent);

        mockMvc.perform(multipart("/api/documents").file(file))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    @Test
    void error_response_does_not_contain_stack_trace() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "bad.pdf", "application/pdf", new byte[]{1, 2, 3});

        MvcResult result = mockMvc.perform(multipart("/api/documents").file(file))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain("at com.lifeadmin");
        assertThat(body).doesNotContain("Exception");
        assertThat(body).doesNotContain("stack");
    }

    @Test
    void path_traversal_filename_returns_422() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "../../etc/passwd.pdf", "application/pdf", new byte[]{1, 2, 3, 4});

        mockMvc.perform(multipart("/api/documents").file(file))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private String extractId(String json) {
        // Minimal JSON extraction without importing Jackson in test
        int start = json.indexOf("\"id\":\"") + 6;
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }
}
