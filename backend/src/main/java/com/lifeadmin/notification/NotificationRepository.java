package com.lifeadmin.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findAllByUserIdOrderBySentAtDesc(UUID userId);
    
    // To prevent duplicate reminders
    boolean existsByActionIdAndCategoryAndType(UUID actionId, NotificationCategory category, NotificationType type);
}
