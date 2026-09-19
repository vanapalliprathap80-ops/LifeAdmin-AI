package com.lifeadmin.integration.dto;

import com.lifeadmin.integration.ConnectionStatus;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class ProviderConnectionDto {
    private UUID id;
    private String providerName;
    private String providerAccountId;
    private ConnectionStatus status;
    private Instant lastSyncedAt;
    private Instant createdAt;
}
