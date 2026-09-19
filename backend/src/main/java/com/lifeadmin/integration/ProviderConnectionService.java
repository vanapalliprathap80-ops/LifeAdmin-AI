package com.lifeadmin.integration;

import com.lifeadmin.integration.dto.ProviderConnectionDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.lifeadmin.notification.NotificationService;
import com.lifeadmin.notification.NotificationCategory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProviderConnectionService {

    private final ProviderConnectionRepository repository;
    private final com.lifeadmin.integration.provider.ProviderRegistry providerRegistry;
    private final NotificationService notificationService;

    public ProviderConnectionService(ProviderConnectionRepository repository,
                                     com.lifeadmin.integration.provider.ProviderRegistry providerRegistry,
                                     NotificationService notificationService) {
        this.repository = repository;
        this.providerRegistry = providerRegistry;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<ProviderConnectionDto> getUserConnections(UUID userId) {
        return repository.findAllByUserId(userId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProviderConnectionDto connect(UUID userId, String providerName, String authCode, java.util.Map<String, String> credentials) {
        // Step 1: Check if this provider actually supports automatic sync
        com.lifeadmin.integration.provider.ConnectedServiceProvider provider = providerRegistry.getProvider(providerName);
        if (!provider.supportsAutomaticSync()) {
            throw new UnsupportedOperationException(
                    "Automatic connection is not available for " + providerName + " yet. Please add this service manually instead.");
        }

        // Step 2: Only reach here if a REAL provider implementation exists
        Optional<ProviderConnection> existingOpt = repository.findByUserIdAndProviderName(userId, providerName);
        
        ProviderConnection connection = existingOpt.orElseGet(ProviderConnection::new);
        connection.setUserId(userId);
        connection.setProviderName(providerName);
        connection.setStatus(ConnectionStatus.CONNECTED); // Mark as connected since it's a sync stub
        connection.setLastSyncedAt(Instant.now());
        
        if (credentials != null && !credentials.isEmpty()) {
            connection.setProviderAccountId(credentials.values().stream().findFirst().orElse(null));
        }
        
        connection = repository.save(connection);
        
        // Trigger real sync with credentials
        if (credentials != null && !credentials.isEmpty()) {
            provider.sync(connection, credentials);
        } else {
            provider.sync(connection);
        }
        
        notificationService.createInAppNotification(userId, null, NotificationCategory.SYSTEM_ALERT, "Successfully connected to " + providerRegistry.getProvider(providerName).getDisplayName());
        
        return mapToDto(connection);
    }

    @Transactional
    public ProviderConnectionDto reSync(UUID userId, UUID connectionId) {
        ProviderConnection connection = repository.findById(connectionId)
                .orElseThrow(() -> new IllegalArgumentException("Connection not found"));
                
        if (!connection.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Connection not found");
        }

        com.lifeadmin.integration.provider.ConnectedServiceProvider provider = providerRegistry.getProvider(connection.getProviderName());
        provider.sync(connection); // Use default/saved credentials logic
        connection.setLastSyncedAt(Instant.now());
        repository.save(connection);

        notificationService.createInAppNotification(userId, null, NotificationCategory.SYSTEM_ALERT, "Synced data from " + provider.getDisplayName());

        return mapToDto(connection);
    }

    @Transactional
    public void disconnect(UUID userId, UUID connectionId) {
        ProviderConnection connection = repository.findById(connectionId)
                .orElseThrow(() -> new IllegalArgumentException("Connection not found"));
                
        if (!connection.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Connection not found");
        }
        
        // In real life, might also revoke token at the provider
        repository.delete(connection);
        notificationService.createInAppNotification(userId, null, NotificationCategory.SYSTEM_ALERT, "Disconnected provider account");
    }

    private ProviderConnectionDto mapToDto(ProviderConnection entity) {
        ProviderConnectionDto dto = new ProviderConnectionDto();
        dto.setId(entity.getId());
        dto.setProviderName(entity.getProviderName());
        dto.setProviderAccountId(entity.getProviderAccountId());
        dto.setStatus(entity.getStatus());
        dto.setLastSyncedAt(entity.getLastSyncedAt());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}
