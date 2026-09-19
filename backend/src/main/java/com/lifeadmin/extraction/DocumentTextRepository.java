package com.lifeadmin.extraction;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DocumentTextRepository extends JpaRepository<DocumentText, UUID> {

    Optional<DocumentText> findByDocumentId(UUID documentId);
}
