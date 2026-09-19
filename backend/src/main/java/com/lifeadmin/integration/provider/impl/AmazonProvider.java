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
public class AmazonProvider implements ConnectedServiceProvider {

    private final UserServiceRepository userServiceRepository;

    public AmazonProvider(UserServiceRepository userServiceRepository) {
        this.userServiceRepository = userServiceRepository;
    }

    @Override public String getProviderName() { return "amazon-prime"; }
    @Override public String getDisplayName() { return "Amazon Prime"; }
    @Override public String getCategory() { return "Shopping"; }
    @Override public String getDescription() { return "Track Amazon Prime membership, Kindle Unlimited, and Subscribe & Save deliveries."; }
    @Override public String getOfficialPortalUrl() { return "https://amazon.com"; }
    
    @Override 
    public void sync(ProviderConnection connection) {
        sync(connection, Map.of("email", "demo@amazon.com"));
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
            service.setName("Amazon Prime");
        }

        service.setAssetName("Annual Membership");
        service.setServiceType(ServiceType.SUBSCRIPTION);
        service.setRenewalDate(LocalDate.now().plusMonths(4).withDayOfMonth(5));
        service.setPrice(java.math.BigDecimal.valueOf(139.00));
        service.setOfficialUrl(getOfficialPortalUrl());
        userServiceRepository.save(service);
    }
    
    @Override public boolean supportsAutomaticSync() { return true; }
}
