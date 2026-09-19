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
public class DigiLockerGovIdProvider implements ConnectedServiceProvider {

    private final UserServiceRepository userServiceRepository;
    private final ActionRepository actionRepository;

    public DigiLockerGovIdProvider(UserServiceRepository userServiceRepository, ActionRepository actionRepository) {
        this.userServiceRepository = userServiceRepository;
        this.actionRepository = actionRepository;
    }

    @Override
    public String getProviderName() {
        return "GOV_ID";
    }

    @Override
    public String getDisplayName() {
        return "DigiLocker / Gov ID Vault";
    }

    @Override
    public String getCategory() {
        return "GOVERNMENT";
    }

    @Override
    public String getDescription() {
        return "Sync Driving License and Passport expiry dates.";
    }

    @Override
    public String getOfficialPortalUrl() {
        return "https://www.digilocker.gov.in/";
    }

    @Override
    public boolean supportsAutomaticSync() {
        return true;
    }

    @Override
    public void sync(ProviderConnection connection) {
        sync(connection, Map.of("aadhaar", "DEMO-XXXX"));
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
            service.setName("Gov ID Vault");
            service.setServiceType(ServiceType.SUBSCRIPTION);
        }

        service.setAssetName("Driving License");
        service.setRenewalDate(LocalDate.now().plusMonths(3));
        service.setOfficialUrl(getOfficialPortalUrl());
        userServiceRepository.save(service);

        Action renewalAction = new Action();
        renewalAction.setUserId(connection.getUserId());
        renewalAction.setTitle("Renew Driving License");
        renewalAction.setDescription("Your driving license expires in 3 months. Apply for renewal online.");
        renewalAction.setDeadline(LocalDate.now().plusDays(15));
        renewalAction.setPriority(com.lifeadmin.action.ActionPriority.HIGH);
        renewalAction.setStatus(com.lifeadmin.action.ActionStatus.PENDING);
        renewalAction.setReason("Synced from DigiLocker");
        renewalAction.setEvidence("Expiry Date: " + LocalDate.now().plusMonths(3).toString());
        actionRepository.save(renewalAction);
    }
}
