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
public class FlightBookingProvider implements ConnectedServiceProvider {

    private final UserServiceRepository userServiceRepository;
    private final ActionRepository actionRepository;

    public FlightBookingProvider(UserServiceRepository userServiceRepository, ActionRepository actionRepository) {
        this.userServiceRepository = userServiceRepository;
        this.actionRepository = actionRepository;
    }

    @Override
    public String getProviderName() {
        return "FLIGHT_SYNC";
    }

    @Override
    public String getDisplayName() {
        return "Airlines Flight Sync";
    }

    @Override
    public String getCategory() {
        return "TRAVEL";
    }

    @Override
    public String getDescription() {
        return "Sync upcoming flight PNRs and check-in times.";
    }

    @Override
    public String getOfficialPortalUrl() {
        return "https://www.united.com/en/us/checkin";
    }

    @Override
    public boolean supportsAutomaticSync() {
        return true;
    }

    @Override
    public void sync(ProviderConnection connection) {
        sync(connection, Map.of("pnr", "DEMO-XYZ"));
    }

    @Override
    public void sync(ProviderConnection connection, Map<String, String> credentials) {
        String pnr = credentials.getOrDefault("pnr", "XYZ987");

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
            service.setName("Flight Booking");
            service.setServiceType(ServiceType.SUBSCRIPTION); // Or TRAVEL if enum exists
        }

        service.setAssetName("PNR: " + pnr);
        service.setRenewalDate(LocalDate.now().plusDays(2));
        service.setOfficialUrl(getOfficialPortalUrl());
        userServiceRepository.save(service);

        Action checkinAction = new Action();
        checkinAction.setUserId(connection.getUserId());
        checkinAction.setTitle("Web Check-in for Flight (PNR " + pnr + ")");
        checkinAction.setDescription("Web check-in opens 24 hours before your departure.");
        checkinAction.setDeadline(LocalDate.now().plusDays(1));
        checkinAction.setPriority(com.lifeadmin.action.ActionPriority.HIGH);
        checkinAction.setStatus(com.lifeadmin.action.ActionStatus.PENDING);
        checkinAction.setReason("Synced from Airlines Provider");
        checkinAction.setEvidence("Departure: " + LocalDate.now().plusDays(2).toString());
        actionRepository.save(checkinAction);
    }
}
