package com.lifeadmin.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for UserService entities (service/subscription).
 */
public record UserServiceDto(
        UUID id,
        String name,
        String currentPlan,
        BigDecimal price,
        String billingFrequency,
        LocalDate renewalDate,
        String officialUrl,
        String notes,
        String serviceType,
        String assetName,
        UUID providerConnectionId,
        Instant createdAt,
        Instant updatedAt
) {
    public static UserServiceDto from(UserService entity) {
        return new UserServiceDto(
                entity.getId(),
                entity.getName(),
                entity.getCurrentPlan(),
                entity.getPrice(),
                entity.getBillingFrequency() != null ? entity.getBillingFrequency().name() : null,
                entity.getRenewalDate(),
                entity.getOfficialUrl(),
                entity.getNotes(),
                entity.getServiceType() != null ? entity.getServiceType().name() : null,
                entity.getAssetName(),
                entity.getProviderConnectionId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
