package com.lifeadmin.integration.provider.impl;

import com.lifeadmin.action.Action;
import com.lifeadmin.action.ActionPriority;
import com.lifeadmin.action.ActionRepository;
import com.lifeadmin.action.ActionStatus;
import com.lifeadmin.integration.ProviderConnection;
import com.lifeadmin.integration.provider.ConnectedServiceProvider;
import com.lifeadmin.service.ServiceType;
import com.lifeadmin.service.UserService;
import com.lifeadmin.service.UserServiceRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class GoogleCalendarProvider implements ConnectedServiceProvider {

    private final UserServiceRepository userServiceRepository;
    private final ActionRepository actionRepository;

    public GoogleCalendarProvider(UserServiceRepository userServiceRepository, ActionRepository actionRepository) {
        this.userServiceRepository = userServiceRepository;
        this.actionRepository = actionRepository;
    }

    @Override public String getProviderName() { return "google_calendar"; }
    @Override public String getDisplayName() { return "Google Calendar"; }
    @Override public String getCategory() { return "Productivity"; }
    @Override public String getDescription() { return "Sync your LifeAdmin actions to Google Calendar events."; }
    @Override public String getOfficialPortalUrl() { return "https://calendar.google.com"; }

    @Override
    public void sync(ProviderConnection connection) {
        sync(connection, Map.of("email", "demo@gmail.com"));
    }

    @Override
    public void sync(ProviderConnection connection, Map<String, String> credentials) {
        String identifier = credentials.values().stream().findFirst().orElse("demo@gmail.com");
        
        Optional<UserService> existingService = userServiceRepository.findAllByUserIdOrderByCreatedAtDesc(connection.getUserId())
                .stream().filter(s -> s.getProviderConnectionId() != null && s.getProviderConnectionId().equals(connection.getId()))
                .findFirst();

        UserService service = existingService.orElseGet(UserService::new);
        if (service.getId() == null) {
            service.setUserId(connection.getUserId());
            service.setProviderConnectionId(connection.getId());
            service.setName("Google Calendar Sync");
            service.setServiceType(ServiceType.SUBSCRIPTION);
            service.setAssetName(identifier);
            userServiceRepository.save(service);
        }

        // Simulating Google Calendar API response
        createMockEvent(connection.getUserId(), "Doctor Appointment", LocalDate.now().plusDays(5));
        createMockEvent(connection.getUserId(), "Car Service", LocalDate.now().plusDays(14));
    }

    private void createMockEvent(UUID userId, String title, LocalDate deadline) {
        Action action = new Action();
        action.setUserId(userId);
        action.setTitle("GCal: " + title);
        action.setDescription("Imported from Google Calendar");
        action.setDeadline(deadline);
        action.setStatus(ActionStatus.PENDING);
        action.setPriority(ActionPriority.MEDIUM);
        actionRepository.save(action);
    }

    @Override public boolean supportsAutomaticSync() { return true; }
}
