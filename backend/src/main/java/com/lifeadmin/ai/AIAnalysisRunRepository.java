package com.lifeadmin.ai;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AIAnalysisRunRepository extends JpaRepository<AIAnalysisRun, UUID> {

    List<AIAnalysisRun> findByDocumentIdOrderByCreatedAtDesc(UUID documentId);
    
    List<AIAnalysisRun> findByUserIdAndDocumentIdOrderByCreatedAtDesc(UUID userId, UUID documentId);
}
