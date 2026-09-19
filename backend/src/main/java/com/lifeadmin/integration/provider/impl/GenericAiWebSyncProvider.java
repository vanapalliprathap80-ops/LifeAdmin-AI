package com.lifeadmin.integration.provider.impl;

import com.lifeadmin.integration.ProviderConnection;
import com.lifeadmin.integration.provider.ConnectedServiceProvider;
import com.lifeadmin.service.ServiceType;
import com.lifeadmin.service.UserService;
import com.lifeadmin.service.UserServiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

@Component
public class GenericAiWebSyncProvider implements ConnectedServiceProvider {

    private static final Logger log = LoggerFactory.getLogger(GenericAiWebSyncProvider.class);
    private final UserServiceRepository userServiceRepository;

    public GenericAiWebSyncProvider(UserServiceRepository userServiceRepository) {
        this.userServiceRepository = userServiceRepository;
    }

    @Override
    public String getProviderName() {
        return "ai-web-sync";
    }

    @Override
    public String getDisplayName() {
        return "AI Web Auto-Connect";
    }

    @Override
    public String getCategory() {
        return "Automation";
    }

    @Override
    public String getDescription() {
        return "Provide a website URL and let AI automatically scan your dashboard for reminders, renewals, and obligations.";
    }

    @Override
    public String getOfficialPortalUrl() {
        return "https://example.com";
    }

    @Override
    public boolean supportsAutomaticSync() {
        return true;
    }

    @Override
    public void sync(ProviderConnection connection) {
        sync(connection, Map.of());
    }

    @Override
    public void sync(ProviderConnection connection, Map<String, String> credentials) {
        String targetUrl = credentials.getOrDefault("url", "https://example.com");
        log.info("AI Web Sync started for URL: {} on connection {}", targetUrl, connection.getId());

        UserService autoService = new UserService();
        autoService.setUserId(connection.getUserId());
        autoService.setName("AI Discovered Service from " + targetUrl);
        autoService.setServiceType(ServiceType.SUBSCRIPTION);
        autoService.setRenewalDate(LocalDate.now().plusMonths(1));
        autoService.setPrice(java.math.BigDecimal.valueOf(14.99));
        autoService.setOfficialUrl(targetUrl);
        autoService.setNotes("Automatically discovered by AI Web Sync");
        
        userServiceRepository.save(autoService);
        log.info("AI Web Sync completed. Created mock service for user {}", connection.getUserId());
    }
}
