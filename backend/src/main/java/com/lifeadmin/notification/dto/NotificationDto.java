package com.lifeadmin.notification.dto;

import com.lifeadmin.notification.NotificationCategory;
import com.lifeadmin.notification.NotificationStatus;
import com.lifeadmin.notification.NotificationType;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class NotificationDto {
    private UUID id;
    private UUID actionId;
    private NotificationType type;
    private NotificationCategory category;
    private NotificationStatus status;
    private String messageContent;
    private Instant sentAt;
}
