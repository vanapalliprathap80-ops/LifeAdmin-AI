package com.lifeadmin.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StorageServiceTest {

    @TempDir
    Path tempDir;

    private StorageService storageService;

    @BeforeEach
    void setUp() {
        StorageProperties props = new StorageProperties();
        props.setUploadDir(tempDir.toString());
        storageService = new StorageService(props);
        storageService.init();
    }

    @Test
    void stored_file_uses_uuid_name_not_original() throws IOException {
        MockMultipartFile file = fakePdf("user-supplied-name.pdf");

        String storedFilename = storageService.store(file);

        // Must not match original name
        assertThat(storedFilename).doesNotContain("user-supplied-name");
        // Must end with .pdf
        assertThat(storedFilename).endsWith(".pdf");
        // Must be a UUID + .pdf pattern (36 chars + 4 = 40)
        assertThat(storedFilename).hasSize(40);
    }

    @Test
    void stored_file_exists_within_upload_root() throws IOException {
        MockMultipartFile file = fakePdf("document.pdf");

        String storedFilename = storageService.store(file);
        Path resolved = storageService.resolve(storedFilename);

        assertThat(resolved).exists();
        assertThat(resolved).startsWith(tempDir);
    }

    @Test
    void original_filename_cannot_escape_storage_root_via_traversal() {
        // Simulates what would happen if a malicious storedFilename was ever passed to resolve()
        assertThatThrownBy(() -> storageService.resolve("../../etc/passwd"))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("traversal");
    }

    @Test
    void stored_file_content_matches_original() throws IOException {
        byte[] content = "hello pdf content".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", content);

        String storedFilename = storageService.store(file);
        byte[] stored = Files.readAllBytes(storageService.resolve(storedFilename));

        assertThat(stored).isEqualTo(content);
    }

    @Test
    void delete_removes_stored_file() throws IOException {
        MockMultipartFile file = fakePdf("toDelete.pdf");
        String storedFilename = storageService.store(file);
        assertThat(storageService.resolve(storedFilename)).exists();

        storageService.delete(storedFilename);

        assertThat(storageService.resolve(storedFilename)).doesNotExist();
    }

    private MockMultipartFile fakePdf(String name) {
        return new MockMultipartFile("file", name, "application/pdf",
                new byte[]{37, 80, 68, 70, 45}); // "%PDF-"
    }
}
