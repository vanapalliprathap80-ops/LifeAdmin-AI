package com.lifeadmin.notification;

import com.lifeadmin.auth.User;
import com.lifeadmin.auth.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void createInAppNotification(UUID userId, UUID actionId, NotificationCategory category, String message) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setActionId(actionId); // can be null
        notification.setType(NotificationType.IN_APP);
        notification.setCategory(category);
        notification.setStatus(NotificationStatus.DELIVERED);
        notification.setMessageContent(message);
        
        notificationRepository.save(notification);
        log.info("Created IN_APP notification for user {}: {}", userId, message);
    }
}
