package com.lifeadmin.integration.provider.impl;

import com.lifeadmin.integration.ProviderConnection;
import com.lifeadmin.integration.provider.ConnectedServiceProvider;
import com.lifeadmin.service.UserService;
import com.lifeadmin.service.UserServiceRepository;
import com.lifeadmin.service.ServiceType;
import com.lifeadmin.action.Action;
import com.lifeadmin.action.ActionRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

@Component
public class ElectricityUtilityProvider implements ConnectedServiceProvider {

    private final UserServiceRepository userServiceRepository;
    private final ActionRepository actionRepository;

    public ElectricityUtilityProvider(UserServiceRepository userServiceRepository, ActionRepository actionRepository) {
        this.userServiceRepository = userServiceRepository;
        this.actionRepository = actionRepository;
    }

    @Override
    public String getProviderName() {
        return "ELECTRICITY_BOARD";
    }

    @Override
    public String getDisplayName() {
        return "State Electricity Board";
    }

    @Override
    public String getCategory() {
        return "UTILITY";
    }

    @Override
    public String getDescription() {
        return "Sync your monthly power bills and due dates.";
    }

    @Override
    public String getOfficialPortalUrl() {
        return "https://www.coned.com/en/login";
    }

    @Override
    public boolean supportsAutomaticSync() {
        return true;
    }

    @Override
    public void sync(ProviderConnection connection) {
        sync(connection, Map.of("accountNumber", "DEMO-123456"));
    }

    @Override
    public void sync(ProviderConnection connection, Map<String, String> credentials) {
        String accountNumber = credentials.getOrDefault("accountNumber", "DEMO-123456");

        // 1. Create or update the Service record
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
            service.setName("Electricity Bill");
            service.setServiceType(ServiceType.SUBSCRIPTION);
        }

        service.setAssetName("Meter " + accountNumber);
        service.setPrice(java.math.BigDecimal.valueOf(145.20));
        service.setRenewalDate(LocalDate.now().plusDays(5));
        service.setOfficialUrl(getOfficialPortalUrl());
        userServiceRepository.save(service);

        // 2. Create an Action for the upcoming bill
        Action billAction = new Action();
        billAction.setUserId(connection.getUserId());
        billAction.setTitle("Pay Electricity Bill (Meter " + accountNumber + ")");
        billAction.setDescription("Your monthly electricity bill of $145.20 is due soon to avoid disconnection.");
        billAction.setDeadline(LocalDate.now().plusDays(5));
        billAction.setPriority(com.lifeadmin.action.ActionPriority.HIGH);
        billAction.setStatus(com.lifeadmin.action.ActionStatus.PENDING);
        billAction.setReason("Synced from State Electricity Board");
        billAction.setEvidence("Amount Due: $145.20");
        actionRepository.save(billAction);
    }
}
