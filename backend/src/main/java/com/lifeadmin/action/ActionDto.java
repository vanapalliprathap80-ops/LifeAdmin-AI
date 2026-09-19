package com.lifeadmin.action;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class ActionDto {
    private UUID id;
    private UUID documentId;
    private UUID obligationId;
    private String title;
    private String description;
    private LocalDate deadline;
    private LocalDate recommendedDate;
    private ActionPriority priority;
    private ActionStatus status;
    private String reason;
    private String evidence;
    private Instant createdAt;
    private Instant completedAt;

    public static ActionDto fromEntity(Action action) {
        ActionDto dto = new ActionDto();
        dto.setId(action.getId());
        dto.setDocumentId(action.getDocument() != null ? action.getDocument().getId() : null);
        dto.setObligationId(action.getObligation() != null ? action.getObligation().getId() : null);
        dto.setTitle(action.getTitle());
        dto.setDescription(action.getDescription());
        dto.setDeadline(action.getDeadline());
        dto.setRecommendedDate(action.getRecommendedDate());
        dto.setPriority(action.getPriority());
        dto.setStatus(action.getStatus());
        dto.setReason(action.getReason());
        dto.setEvidence(action.getEvidence());
        dto.setCreatedAt(action.getCreatedAt());
        dto.setCompletedAt(action.getCompletedAt());
        return dto;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getDocumentId() { return documentId; }
    public void setDocumentId(UUID documentId) { this.documentId = documentId; }
    public UUID getObligationId() { return obligationId; }
    public void setObligationId(UUID obligationId) { this.obligationId = obligationId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }
    public LocalDate getRecommendedDate() { return recommendedDate; }
    public void setRecommendedDate(LocalDate recommendedDate) { this.recommendedDate = recommendedDate; }
    public ActionPriority getPriority() { return priority; }
    public void setPriority(ActionPriority priority) { this.priority = priority; }
    public ActionStatus getStatus() { return status; }
    public void setStatus(ActionStatus status) { this.status = status; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getEvidence() { return evidence; }
    public void setEvidence(String evidence) { this.evidence = evidence; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
