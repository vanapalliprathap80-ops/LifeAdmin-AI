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
public class SpotifyProvider implements ConnectedServiceProvider {

    private final UserServiceRepository userServiceRepository;

    public SpotifyProvider(UserServiceRepository userServiceRepository) {
        this.userServiceRepository = userServiceRepository;
    }

    @Override public String getProviderName() { return "spotify"; }
    @Override public String getDisplayName() { return "Spotify"; }
    @Override public String getCategory() { return "Entertainment"; }
    @Override public String getDescription() { return "Track your Spotify Premium subscription and billing cycle."; }
    @Override public String getOfficialPortalUrl() { return "https://spotify.com"; }
    
    @Override 
    public void sync(ProviderConnection connection) {
        sync(connection, Map.of("email", "demo@spotify.com"));
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
            service.setName("Spotify");
        }

        service.setAssetName("Premium Individual");
        service.setServiceType(ServiceType.SUBSCRIPTION);
        service.setRenewalDate(LocalDate.now().plusMonths(1).withDayOfMonth(15));
        service.setPrice(java.math.BigDecimal.valueOf(11.99));
        service.setOfficialUrl(getOfficialPortalUrl());
        userServiceRepository.save(service);
    }
    
    @Override public boolean supportsAutomaticSync() { return true; }
}
