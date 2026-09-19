package com.lifeadmin.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request body for creating/updating a service entry.
 */
public record CreateServiceRequest(
        @NotBlank(message = "Service name is required")
        @Size(max = 200, message = "Service name must be 200 characters or fewer")
        String name,

        @Size(max = 200, message = "Plan name must be 200 characters or fewer")
        String currentPlan,

        BigDecimal price,

        String billingFrequency,

        LocalDate renewalDate,

        String officialUrl,

        String notes,

        String serviceType,

        @Size(max = 255, message = "Asset name must be 255 characters or fewer")
        String assetName
) {}
