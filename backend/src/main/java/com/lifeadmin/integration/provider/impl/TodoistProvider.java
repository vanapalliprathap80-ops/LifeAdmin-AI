package com.lifeadmin.integration.provider.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifeadmin.action.Action;
import com.lifeadmin.action.ActionPriority;
import com.lifeadmin.action.ActionRepository;
import com.lifeadmin.action.ActionStatus;
import com.lifeadmin.integration.ProviderConnection;
import com.lifeadmin.integration.provider.ConnectedServiceProvider;
import com.lifeadmin.service.ServiceType;
import com.lifeadmin.service.UserService;
import com.lifeadmin.service.UserServiceRepository;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class TodoistProvider implements ConnectedServiceProvider {

    private final UserServiceRepository userServiceRepository;
    private final ActionRepository actionRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public TodoistProvider(UserServiceRepository userServiceRepository, ActionRepository actionRepository) {
        this.userServiceRepository = userServiceRepository;
        this.actionRepository = actionRepository;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    @Override public String getProviderName() { return "todoist"; }
    @Override public String getDisplayName() { return "Todoist"; }
    @Override public String getCategory() { return "Productivity"; }
    @Override public String getDescription() { return "Sync your Todoist tasks directly into LifeAdmin."; }
    @Override public String getOfficialPortalUrl() { return "https://todoist.com"; }

    @Override
    public void sync(ProviderConnection connection) {
        sync(connection, Map.of("token", "DEMO"));
    }

    @Override
    public void sync(ProviderConnection connection, Map<String, String> credentials) {
        String token = credentials.values().stream().findFirst().orElse("DEMO");
        
        Optional<UserService> existingService = userServiceRepository.findAllByUserIdOrderByCreatedAtDesc(connection.getUserId())
                .stream().filter(s -> s.getProviderConnectionId() != null && s.getProviderConnectionId().equals(connection.getId()))
                .findFirst();

        UserService service = existingService.orElseGet(UserService::new);
        if (service.getId() == null) {
            service.setUserId(connection.getUserId());
            service.setProviderConnectionId(connection.getId());
            service.setName("Todoist Integration");
            service.setServiceType(ServiceType.SUBSCRIPTION);
            service.setAssetName("Task Sync");
            userServiceRepository.save(service);
        }

        if (token.equalsIgnoreCase("DEMO")) {
            createMockTask(connection.getUserId(), "Buy groceries", LocalDate.now().plusDays(1));
            createMockTask(connection.getUserId(), "Call insurance company", LocalDate.now().plusDays(2));
            return;
        }

        // Real API Call
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<String> entity = new HttpEntity<>("parameters", headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                    "https://api.todoist.com/rest/v2/tasks",
                    HttpMethod.GET,
                    entity,
                    String.class
            );
            
            List<Map<String, Object>> tasks = objectMapper.readValue(response.getBody(), new TypeReference<List<Map<String, Object>>>() {});
            
            for (Map<String, Object> task : tasks) {
                String content = (String) task.get("content");
                Action action = new Action();
                action.setUserId(connection.getUserId());
                action.setTitle("Todoist: " + content);
                action.setDescription("Imported from Todoist");
                action.setStatus(ActionStatus.PENDING);
                action.setPriority(ActionPriority.MEDIUM);
                
                if (task.containsKey("due") && task.get("due") != null) {
                    Map<String, Object> due = (Map<String, Object>) task.get("due");
                    String dateStr = (String) due.get("date");
                    if (dateStr != null && !dateStr.isEmpty()) {
                        action.setDeadline(LocalDate.parse(dateStr.substring(0, 10)));
                    }
                }
                actionRepository.save(action);
            }
        } catch (Exception e) {
            System.err.println("Todoist sync failed: " + e.getMessage());
            createMockTask(connection.getUserId(), "Failed to sync Todoist API", LocalDate.now());
        }
    }

    private void createMockTask(UUID userId, String title, LocalDate deadline) {
        Action action = new Action();
        action.setUserId(userId);
        action.setTitle("Todoist: " + title);
        action.setDescription("Imported mock task from Todoist");
        action.setDeadline(deadline);
        action.setStatus(ActionStatus.PENDING);
        action.setPriority(ActionPriority.MEDIUM);
        actionRepository.save(action);
    }

    @Override public boolean supportsAutomaticSync() { return true; }
}
