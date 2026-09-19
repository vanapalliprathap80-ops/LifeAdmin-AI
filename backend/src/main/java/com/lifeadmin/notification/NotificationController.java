package com.lifeadmin.notification;

import com.lifeadmin.auth.User;
import com.lifeadmin.auth.UserRepository;
import com.lifeadmin.notification.dto.NotificationDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationController(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<NotificationDto>> getNotifications(@AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = getUserId(userDetails);
        
        List<NotificationDto> dtos = notificationRepository.findAllByUserIdOrderBySentAtDesc(userId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
                
        return ResponseEntity.ok(dtos);
    }

    private NotificationDto mapToDto(Notification entity) {
        NotificationDto dto = new NotificationDto();
        dto.setId(entity.getId());
        dto.setActionId(entity.getActionId());
        dto.setType(entity.getType());
        dto.setCategory(entity.getCategory());
        dto.setStatus(entity.getStatus());
        dto.setMessageContent(entity.getMessageContent());
        dto.setSentAt(entity.getSentAt());
        return dto;
    }

    private UUID getUserId(UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getId();
    }
}
