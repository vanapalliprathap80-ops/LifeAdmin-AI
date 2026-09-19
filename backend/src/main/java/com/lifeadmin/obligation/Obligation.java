package com.lifeadmin.obligation;

import com.lifeadmin.document.Document;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "obligations")
@Getter
@Setter
@NoArgsConstructor
public class Obligation {

    @Column(name = "user_id", nullable = false)
    private UUID userId;


    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id",
            foreignKey = @ForeignKey(name = "fk_obligations_document"))
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id",
            foreignKey = @ForeignKey(name = "fk_obligations_service"))
    private com.lifeadmin.service.UserService service;

    @Column(name = "obligation_type", length = 100)
    private String obligationType;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "evidence", columnDefinition = "TEXT")
    private String evidence;

    @Column(name = "confidence", precision = 5, scale = 4)
    private BigDecimal confidence;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ObligationStatus status = ObligationStatus.IDENTIFIED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
