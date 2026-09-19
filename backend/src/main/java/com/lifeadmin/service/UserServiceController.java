package com.lifeadmin.service;

import com.lifeadmin.auth.User;
import com.lifeadmin.common.ApiResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.lifeadmin.notification.NotificationService;
import com.lifeadmin.notification.NotificationCategory;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for user-managed services/subscriptions.
 * Lightweight CRUD — no credentials collected, no external integrations.
 */
@RestController
@RequestMapping("/api/services")
public class UserServiceController {

    private static final Logger log = LoggerFactory.getLogger(UserServiceController.class);

    private final UserServiceRepository repository;
    private final NotificationService notificationService;

    public UserServiceController(UserServiceRepository repository, NotificationService notificationService) {
        this.repository = repository;
        this.notificationService = notificationService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserServiceDto>> create(
            @Valid @RequestBody CreateServiceRequest req, @AuthenticationPrincipal User user) {

        log.info("Creating service entry: {}", req.name());

        UserService entity = new UserService();
        entity.setUserId(user.getId());
        entity.setName(req.name());
        entity.setCurrentPlan(req.currentPlan());
        entity.setPrice(req.price());
        if (req.billingFrequency() != null && !req.billingFrequency().isBlank()) {
            entity.setBillingFrequency(BillingFrequency.valueOf(req.billingFrequency()));
        }
        entity.setRenewalDate(req.renewalDate());
        entity.setOfficialUrl(req.officialUrl());
        entity.setNotes(req.notes());
        
        if (req.serviceType() != null && !req.serviceType().isBlank()) {
            entity.setServiceType(ServiceType.valueOf(req.serviceType()));
        } else {
            entity.setServiceType(ServiceType.SUBSCRIPTION);
        }
        entity.setAssetName(req.assetName());

        UserService saved = repository.save(entity);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(UserServiceDto.from(saved)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserServiceDto>>> listAll(@AuthenticationPrincipal User user) {
        List<UserServiceDto> list = repository.findAllByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(UserServiceDto::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserServiceDto>> getById(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        UserService entity = repository.findById(id)
                .orElseThrow(() -> new com.lifeadmin.common.ResourceNotFoundException("Service", id));
        if (!entity.getUserId().equals(user.getId())) {
             throw new com.lifeadmin.common.ResourceNotFoundException("Service", id);
        }
        return ResponseEntity.ok(ApiResponse.ok(UserServiceDto.from(entity)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        UserService entity = repository.findById(id)
                .orElseThrow(() -> new com.lifeadmin.common.ResourceNotFoundException("Service", id));
        if (!entity.getUserId().equals(user.getId())) {
            throw new com.lifeadmin.common.ResourceNotFoundException("Service", id);
        }
        repository.deleteById(id);
        notificationService.createInAppNotification(user.getId(), null, NotificationCategory.SYSTEM_ALERT, "Service deleted successfully.");
        return ResponseEntity.noContent().build();
    }
}
