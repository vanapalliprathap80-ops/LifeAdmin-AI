package com.lifeadmin.storage;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Responsible ONLY for safely persisting uploaded files to the local filesystem.
 *
 * Security guarantees:
 *  - Generated UUID-based filenames are used; user-supplied filenames are never used as paths.
 *  - All stored files are confined to the configured upload root.
 *  - The resolved path is validated to be inside the root (path traversal protection).
 *  - Internal paths are never returned to callers (the returned value is the filename only).
 */
@Service
public class StorageService {

    private static final Logger log = LoggerFactory.getLogger(StorageService.class);

    private final Path uploadRoot;

    public StorageService(StorageProperties properties) {
        // Resolve relative to the current working directory (the project/backend dir).
        // Env-var UPLOAD_DIR can override this to an absolute path.
        this.uploadRoot = Paths.get(properties.getUploadDir()).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(uploadRoot);
            log.info("Storage root initialised: {}", uploadRoot);
        } catch (IOException e) {
            throw new StorageException("Cannot initialise storage directory", e);
        }
    }

    /**
     * Stores the multipart file safely.
     *
     * @return the server-generated filename (UUID + ".pdf")
     * @throws StorageException if the file cannot be written
     */
    public String store(MultipartFile file) {
        String storedFilename = UUID.randomUUID() + ".pdf";
        Path destination = resolveAndValidate(storedFilename);

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
            log.info("Stored file as {} ({} bytes)", storedFilename, file.getSize());
            return storedFilename;
        } catch (IOException e) {
            throw new StorageException("Failed to store file", e);
        }
    }

    /**
     * Returns the full Path of a stored file (for internal backend use only).
     * Never expose this path through the API.
     */
    public Path resolve(String storedFilename) {
        return resolveAndValidate(storedFilename);
    }

    /**
     * Deletes a stored file. Swallows errors — used for cleanup on processing failure.
     */
    public void delete(String storedFilename) {
        try {
            Path target = resolveAndValidate(storedFilename);
            Files.deleteIfExists(target);
            log.info("Deleted stored file: {}", storedFilename);
        } catch (Exception e) {
            log.warn("Could not delete stored file {}: {}", storedFilename, e.getMessage());
        }
    }

    // ─── private ──────────────────────────────────────────────────────────────

    /**
     * Resolves {@code filename} within the upload root and validates it stays inside.
     * Protects against path traversal (../../ etc).
     */
    private Path resolveAndValidate(String filename) {
        Path resolved = uploadRoot.resolve(filename).normalize();
        if (!resolved.startsWith(uploadRoot)) {
            throw new StorageException("Path traversal attempt detected for: " + filename);
        }
        return resolved;
    }
}
