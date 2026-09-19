package com.lifeadmin.extraction;

import com.lifeadmin.document.Document;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "document_texts")
@Getter
@Setter
@NoArgsConstructor
public class DocumentText {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    // Owned side: document_texts.document_id is the FK column.
    // Unidirectional: Document does not need to navigate back to DocumentText.
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false, unique = true,
            foreignKey = @ForeignKey(name = "fk_document_texts_document"))
    private Document document;

    @Column(name = "extracted_text", columnDefinition = "TEXT")
    private String extractedText;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
