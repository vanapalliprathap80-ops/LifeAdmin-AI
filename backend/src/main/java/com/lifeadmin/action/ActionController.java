package com.lifeadmin.action;

import com.lifeadmin.auth.User;
import com.lifeadmin.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Handles HTTP concerns only for the Action Center.
 * Business logic is delegated to ActionService.
 */
@RestController
@RequestMapping("/api/actions")
public class ActionController {

    private final ActionService actionService;

    public ActionController(ActionService actionService) {
        this.actionService = actionService;
    }

    /**
     * GET /api/actions
     * Returns actions with optional filtering by status and priority.
     * Results are deterministically ordered: pending before completed,
     * higher priority first, earlier deadline first.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ActionDto>>> listActions(
            @RequestParam(required = false) ActionStatus status,
            @RequestParam(required = false) ActionPriority priority,
            @AuthenticationPrincipal User user) {

        List<ActionDto> dtos = actionService.getActions(user.getId(), status, priority).stream()
                .map(ActionDto::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.ok(dtos));
    }

    /**
     * PATCH /api/actions/{id}/complete
     * Marks an action as completed. Idempotent — safe to call multiple times.
     */
    @PatchMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<ActionDto>> markActionCompleted(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        Action action = actionService.completeAction(id, user.getId());
        return ResponseEntity.ok(ApiResponse.ok(ActionDto.fromEntity(action)));
    }
}
