package com.lifeadmin.service;

import com.lifeadmin.action.Action;
import com.lifeadmin.action.ActionPriority;
import com.lifeadmin.action.ActionRepository;
import com.lifeadmin.action.ActionStatus;
import com.lifeadmin.obligation.Obligation;
import com.lifeadmin.obligation.ObligationRepository;
import com.lifeadmin.obligation.ObligationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Periodically scans UserService records (both manual and automated)
 * to detect expiring warranties or upcoming subscription renewals,
 * generating Obligations and Actions for the user.
 */
@Component
public class ServiceObligationEngine {

    private static final Logger log = LoggerFactory.getLogger(ServiceObligationEngine.class);

    private final UserServiceRepository userServiceRepository;
    private final ObligationRepository obligationRepository;
    private final ActionRepository actionRepository;

    public ServiceObligationEngine(UserServiceRepository userServiceRepository,
                                   ObligationRepository obligationRepository,
                                   ActionRepository actionRepository) {
        this.userServiceRepository = userServiceRepository;
        this.obligationRepository = obligationRepository;
        this.actionRepository = actionRepository;
    }

    // Run periodically (e.g., daily at midnight: "0 0 0 * * *")
    // For demo purposes, we can run it every minute: "0 * * * * *"
    @Scheduled(cron = "0 * * * * *")
    public void generateObligationsForServices() {
        log.info("Running ServiceObligationEngine...");
        LocalDate today = LocalDate.now();

        List<UserService> services = userServiceRepository.findAll();
        for (UserService service : services) {
            if (service.getRenewalDate() != null) {
                long daysUntil = ChronoUnit.DAYS.between(today, service.getRenewalDate());
                if (daysUntil <= 30 && daysUntil >= 0) {
                    processService(service);
                }
            }
        }
    }

    private void processService(UserService service) {
        // 1. Check if an obligation already exists for this specific service's current renewal date
        // Note: For a robust system we'd track the exact period, but here we just check if any IDENTIFIED obligation exists.
        // Let's assume one active obligation per service at a time.
        
        // This query requires a method in ObligationRepository. We'll add it if it doesn't exist.
        // Using a simpler approach: fetch all and filter for now to avoid needing to modify ObligationRepository immediately.
        List<Obligation> activeObligations = obligationRepository.findAll().stream()
                .filter(o -> o.getService() != null && o.getService().getId().equals(service.getId()))
                .filter(o -> o.getStatus() == ObligationStatus.IDENTIFIED)
                .toList();

        if (!activeObligations.isEmpty()) {
            return; // Already generated
        }

        log.info("Generating Obligation and Action for service: {}", service.getName());

        String obligationType = service.getServiceType() == ServiceType.WARRANTY ? "Warranty Expiry" : "Subscription Renewal";
        
        Obligation obligation = new Obligation();
        obligation.setUserId(service.getUserId());
        obligation.setService(service);
        obligation.setObligationType(obligationType);
        obligation.setDescription("Your " + (service.getAssetName() != null ? service.getAssetName() : service.getName()) + " is approaching its " + obligationType.toLowerCase() + " date.");
        obligation.setStatus(ObligationStatus.IDENTIFIED);
        
        obligation = obligationRepository.save(obligation);

        Action action = new Action();
        action.setUserId(service.getUserId());
        action.setService(service);
        action.setObligation(obligation);
        action.setTitle("Review " + obligationType + " for " + service.getName());
        action.setDescription(obligation.getDescription());
        action.setDeadline(service.getRenewalDate());
        action.setRecommendedDate(LocalDate.now());
        action.setPriority(ActionPriority.HIGH);
        action.setStatus(ActionStatus.PENDING);
        
        if (service.getOfficialUrl() != null && !service.getOfficialUrl().isEmpty()) {
            action.setReason("Official action link available: " + service.getOfficialUrl());
            // Frontend can parse this or we can add a specific officialUrl field to Action in the future.
        }

        actionRepository.save(action);
    }
}
