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
public class GovTaxPortalProvider implements ConnectedServiceProvider {

    private final UserServiceRepository userServiceRepository;
    private final ActionRepository actionRepository;

    public GovTaxPortalProvider(UserServiceRepository userServiceRepository, ActionRepository actionRepository) {
        this.userServiceRepository = userServiceRepository;
        this.actionRepository = actionRepository;
    }

    @Override
    public String getProviderName() {
        return "GOV_TAX";
    }

    @Override
    public String getDisplayName() {
        return "IRS / Government Tax Portal";
    }

    @Override
    public String getCategory() {
        return "GOVERNMENT";
    }

    @Override
    public String getDescription() {
        return "Sync your tax filing obligations and refund status.";
    }

    @Override
    public String getOfficialPortalUrl() {
        return "https://www.irs.gov/";
    }

    @Override
    public boolean supportsAutomaticSync() {
        return true;
    }

    @Override
    public void sync(ProviderConnection connection) {
        sync(connection, Map.of("ssn", "DEMO-0000"));
    }

    @Override
    public void sync(ProviderConnection connection, Map<String, String> credentials) {
        String ssn = credentials.getOrDefault("ssn", "XXX-XX-1234");

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
            service.setName("Tax Compliance");
            service.setServiceType(ServiceType.SUBSCRIPTION); 
        }

        service.setAssetName("TIN: " + ssn);
        service.setRenewalDate(LocalDate.of(LocalDate.now().getYear(), 4, 15));
        if (service.getRenewalDate().isBefore(LocalDate.now())) {
            service.setRenewalDate(LocalDate.of(LocalDate.now().getYear() + 1, 4, 15));
        }
        service.setOfficialUrl(getOfficialPortalUrl());
        userServiceRepository.save(service);

        Action taxAction = new Action();
        taxAction.setUserId(connection.getUserId());
        taxAction.setTitle("File Annual Tax Return");
        taxAction.setDescription("Your annual tax return is due. Ensure all W-2s and 1099s are collected.");
        taxAction.setDeadline(service.getRenewalDate());
        taxAction.setPriority(com.lifeadmin.action.ActionPriority.HIGH);
        taxAction.setStatus(com.lifeadmin.action.ActionStatus.PENDING);
        taxAction.setReason("Synced from Tax Portal");
        taxAction.setEvidence("Filing deadline for current tax year");
        actionRepository.save(taxAction);
    }
}
