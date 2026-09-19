package com.lifeadmin.notification;

import com.lifeadmin.action.Action;
import com.lifeadmin.action.ActionRepository;
import com.lifeadmin.action.ActionStatus;
import com.lifeadmin.auth.User;
import com.lifeadmin.auth.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class DeadlineReminderJob {

    private static final Logger log = LoggerFactory.getLogger(DeadlineReminderJob.class);

    private final ActionRepository actionRepository;
    private final NotificationRepository notificationRepository;
    private final EmailDeliveryService emailDeliveryService;
    private final UserRepository userRepository;

    public DeadlineReminderJob(ActionRepository actionRepository,
                               NotificationRepository notificationRepository,
                               EmailDeliveryService emailDeliveryService,
                               UserRepository userRepository) {
        this.actionRepository = actionRepository;
        this.notificationRepository = notificationRepository;
        this.emailDeliveryService = emailDeliveryService;
        this.userRepository = userRepository;
    }

    // Run every hour. For development, you might want to run it every minute: @Scheduled(cron = "0 * * * * *")
    // Here we use a safe hourly cron, but for the hackathon we can do every minute.
    @Scheduled(cron = "0 * * * * *")
    public void processReminders() {
        log.info("Running DeadlineReminderJob...");
        LocalDate today = LocalDate.now();

        // Get all incomplete actions that have a deadline
        List<Action> pendingActions = actionRepository.findAll().stream()
                .filter(a -> a.getStatus() != ActionStatus.COMPLETED && a.getDeadline() != null)
                .toList();

        for (Action action : pendingActions) {
            long daysUntil = ChronoUnit.DAYS.between(today, action.getDeadline());

            if (daysUntil < 0) {
                checkAndSend(action, NotificationCategory.DEADLINE_OVERDUE);
            } else if (daysUntil == 1) {
                checkAndSend(action, NotificationCategory.DEADLINE_REMINDER_1_DAY);
            } else if (daysUntil == 7) {
                checkAndSend(action, NotificationCategory.DEADLINE_REMINDER_7_DAYS);
            } else if (daysUntil == 30) {
                checkAndSend(action, NotificationCategory.DEADLINE_REMINDER_30_DAYS);
            }
        }
    }

    private void checkAndSend(Action action, NotificationCategory category) {
        // Prevent duplicate sending for the same action/category
        if (notificationRepository.existsByActionIdAndCategoryAndType(action.getId(), category, NotificationType.EMAIL)) {
            return; 
        }

        User user = userRepository.findById(action.getUserId()).orElse(null);
        if (user == null) {
            log.warn("User not found for action {}", action.getId());
            return;
        }

        // Send Email (stub)
        String subject = String.format("LifeAdmin Reminder: %s", action.getTitle());
        String body = String.format("Hi,\n\nThis is a reminder regarding: %s\nDeadline: %s\nPriority: %s\n\nPlease log in to LifeAdmin to take action.",
                action.getTitle(), action.getDeadline(), action.getPriority());

        emailDeliveryService.sendEmail(user.getEmail(), subject, body);

        // Record Email Notification
        Notification notification = new Notification();
        notification.setUserId(user.getId());
        notification.setActionId(action.getId());
        notification.setType(NotificationType.EMAIL);
        notification.setCategory(category);
        notification.setStatus(NotificationStatus.SENT);
        notification.setMessageContent(subject + "\n" + body);
        notificationRepository.save(notification);
        
        // Record In-App Notification
        Notification inAppNotification = new Notification();
        inAppNotification.setUserId(user.getId());
        inAppNotification.setActionId(action.getId());
        inAppNotification.setType(NotificationType.IN_APP);
        inAppNotification.setCategory(category);
        inAppNotification.setStatus(NotificationStatus.DELIVERED);
        inAppNotification.setMessageContent("Reminder: " + action.getTitle() + " is due " + action.getDeadline());
        notificationRepository.save(inAppNotification);
        
        log.info("Sent {} notifications for action {}", category, action.getId());
    }
}
