package com.lifeadmin.action;

import com.lifeadmin.document.Document;
import com.lifeadmin.obligation.Obligation;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "actions")
@Getter
@Setter
@NoArgsConstructor
public class Action {

    @Column(name = "user_id", nullable = false)
    private UUID userId;


    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id",
            foreignKey = @ForeignKey(name = "fk_actions_document"))
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id",
            foreignKey = @ForeignKey(name = "fk_actions_service"))
    private com.lifeadmin.service.UserService service;

    // Optional: an action may or may not stem from a specific obligation
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "obligation_id",
            foreignKey = @ForeignKey(name = "fk_actions_obligation"))
    private Obligation obligation;

    @Column(name = "title", nullable = false, columnDefinition = "TEXT")
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "recommended_date")
    private LocalDate recommendedDate;

    @Column(name = "deadline")
    private LocalDate deadline;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 50)
    private ActionPriority priority = ActionPriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ActionStatus status = ActionStatus.PENDING;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "evidence", columnDefinition = "TEXT")
    private String evidence;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
