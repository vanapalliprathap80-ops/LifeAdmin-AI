package com.lifeadmin.document;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface KeyDateRepository extends JpaRepository<KeyDate, UUID> {

    List<KeyDate> findByDocumentId(UUID documentId);
}
