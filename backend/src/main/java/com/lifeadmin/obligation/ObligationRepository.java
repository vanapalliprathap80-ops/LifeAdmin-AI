package com.lifeadmin.obligation;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ObligationRepository extends JpaRepository<Obligation, UUID> {

    List<Obligation> findByDocumentId(UUID documentId);
    
    List<Obligation> findByUserIdAndDocumentId(UUID userId, UUID documentId);
}
