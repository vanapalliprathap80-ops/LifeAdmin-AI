package com.lifeadmin.service;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Represents a user-managed service or subscription that may not have a backing document.
 * Demonstrates that LifeAdmin can manage obligations even without a PDF.
 */
@Entity
@Table(name = "services")
@Getter
@Setter
@NoArgsConstructor
public class UserService {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "current_plan", length = 200)
    private String currentPlan;

    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_frequency", length = 30)
    private BillingFrequency billingFrequency;

    @Column(name = "renewal_date")
    private LocalDate renewalDate;

    @Column(name = "official_url", length = 500)
    private String officialUrl;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "provider_connection_id")
    private UUID providerConnectionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false, length = 50)
    private ServiceType serviceType = ServiceType.SUBSCRIPTION;

    @Column(name = "asset_name", length = 255)
    private String assetName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
