package com.lifeadmin.integration.provider.impl;

import com.lifeadmin.integration.ProviderConnection;
import com.lifeadmin.integration.provider.ConnectedServiceProvider;
import com.lifeadmin.service.UserService;
import com.lifeadmin.service.UserServiceRepository;
import com.lifeadmin.service.ServiceType;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

@Component
public class StreamingMediaProvider implements ConnectedServiceProvider {

    private final UserServiceRepository userServiceRepository;

    public StreamingMediaProvider(UserServiceRepository userServiceRepository) {
        this.userServiceRepository = userServiceRepository;
    }

    @Override
    public String getProviderName() {
        return "STREAMING";
    }

    @Override
    public String getDisplayName() {
        return "Netflix / Streaming Media";
    }

    @Override
    public String getCategory() {
        return "SUBSCRIPTION";
    }

    @Override
    public String getDescription() {
        return "Sync entertainment subscriptions.";
    }

    @Override
    public String getOfficialPortalUrl() {
        return "https://www.netflix.com/";
    }

    @Override
    public boolean supportsAutomaticSync() {
        return true;
    }

    @Override
    public void sync(ProviderConnection connection) {
        sync(connection, Map.of("email", "demo@example.com"));
    }

    @Override
    public void sync(ProviderConnection connection, Map<String, String> credentials) {
        Optional<UserService> existingService = userServiceRepository.findAllByUserIdOrderByCreatedAtDesc(connection.getUserId())
                .stream().filter(s -> s.getProviderConnectionId() != null && s.getProviderConnectionId().equals(connection.getId()))
                .findFirst();

        UserService service;
        if (existingService.isPresent()) {
            service = existingService.get();
        } else {
            service = new UserService();
            service.setUserId(connection.getUserId());
            service.setProviderConnectionId(connection.getId());
            service.setName("Netflix");
            service.setServiceType(ServiceType.SUBSCRIPTION);
        }

        service.setAssetName("Premium Plan");
        service.setServiceType(ServiceType.SUBSCRIPTION);
        service.setRenewalDate(LocalDate.now().plusMonths(1));
        service.setPrice(java.math.BigDecimal.valueOf(15.99));
        service.setOfficialUrl(getOfficialPortalUrl());
        userServiceRepository.save(service);
    }
}
