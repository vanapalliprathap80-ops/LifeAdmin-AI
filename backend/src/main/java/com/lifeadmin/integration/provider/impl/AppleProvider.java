package com.lifeadmin.integration.provider.impl;
import com.lifeadmin.integration.ProviderConnection;
import com.lifeadmin.integration.provider.ConnectedServiceProvider;
import com.lifeadmin.service.ServiceType;
import com.lifeadmin.service.UserService;
import com.lifeadmin.service.UserServiceRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

@Component
public class AppleProvider implements ConnectedServiceProvider {

    private final UserServiceRepository userServiceRepository;

    public AppleProvider(UserServiceRepository userServiceRepository) {
        this.userServiceRepository = userServiceRepository;
    }

    @Override public String getProviderName() { return "apple-services"; }
    @Override public String getDisplayName() { return "Apple Services"; }
    @Override public String getCategory() { return "Technology"; }
    @Override public String getDescription() { return "Connect your Apple ID to track iCloud, Apple Music, and Apple TV+ subscriptions."; }
    @Override public String getOfficialPortalUrl() { return "https://appleid.apple.com"; }
    
    @Override 
    public void sync(ProviderConnection connection) {
        sync(connection, Map.of("email", "demo@apple.com"));
    }
    
    @Override 
    public void sync(ProviderConnection connection, Map<String, String> credentials) {
        Optional<UserService> existingService = userServiceRepository.findAllByUserIdOrderByCreatedAtDesc(connection.getUserId())
                .stream().filter(s -> s.getProviderConnectionId() != null && s.getProviderConnectionId().equals(connection.getId()))
                .findFirst();

        UserService service = existingService.orElseGet(UserService::new);
        if (service.getId() == null) {
            service.setUserId(connection.getUserId());
            service.setProviderConnectionId(connection.getId());
            service.setName("Apple Music");
        }

        service.setAssetName("Individual Plan");
        service.setServiceType(ServiceType.SUBSCRIPTION);
        service.setRenewalDate(LocalDate.now().plusMonths(1).withDayOfMonth(20));
        service.setPrice(java.math.BigDecimal.valueOf(10.99));
        service.setOfficialUrl(getOfficialPortalUrl());
        userServiceRepository.save(service);
    }
    
    @Override public boolean supportsAutomaticSync() { return true; }
}
