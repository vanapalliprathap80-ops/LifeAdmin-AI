package com.lifeadmin.action;

import com.lifeadmin.common.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.lifeadmin.document.Document;

/**
 * Unit tests for ActionController using standalone MockMvc.
 * Tests HTTP concerns: routing, response structure, status codes, error handling.
 */
class ActionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ActionService actionService;

    private Document document;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Configure ObjectMapper with Java time support for ISO serialization
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper);

        ActionController controller = new ActionController(actionService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(converter)
                .build();

        document = new Document();
        document.setId(UUID.randomUUID());
    }

    private Action buildAction(UUID id, String title, ActionStatus status,
                                ActionPriority priority, LocalDate deadline) {
        Action action = new Action();
        action.setId(id);
        action.setDocument(document);
        action.setTitle(title);
        action.setDescription("Test description for " + title);
        action.setStatus(status);
        action.setPriority(priority);
        action.setDeadline(deadline);
        action.setReason("Test reason");
        action.setEvidence("Test evidence");
        return action;
    }

    // ── GET /api/actions ──────────────────────────────────────────────────

    @Test
    void listActions_returnsActions() throws Exception {
        UUID actionId = UUID.randomUUID();
        Action action = buildAction(actionId, "Pay rent", ActionStatus.PENDING,
                ActionPriority.HIGH, LocalDate.of(2026, 10, 1));

        when(actionService.getActions(null, null)).thenReturn(List.of(action));

        mockMvc.perform(get("/api/actions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(actionId.toString()))
                .andExpect(jsonPath("$.data[0].title").value("Pay rent"))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"))
                .andExpect(jsonPath("$.data[0].priority").value("HIGH"))
                .andExpect(jsonPath("$.data[0].deadline").value("2026-10-01"))
                .andExpect(jsonPath("$.data[0].documentId").value(document.getId().toString()))
                .andExpect(jsonPath("$.data[0].reason").value("Test reason"))
                .andExpect(jsonPath("$.data[0].evidence").value("Test evidence"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void listActions_emptyList_returnsSuccess() throws Exception {
        when(actionService.getActions(null, null)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/actions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void listActions_pendingAndCompleted() throws Exception {
        Action pending = buildAction(UUID.randomUUID(), "Pending", ActionStatus.PENDING,
                ActionPriority.HIGH, LocalDate.of(2026, 10, 1));
        Action completed = buildAction(UUID.randomUUID(), "Completed", ActionStatus.COMPLETED,
                ActionPriority.MEDIUM, LocalDate.of(2026, 9, 1));
        completed.setCompletedAt(Instant.parse("2026-09-15T10:00:00Z"));

        when(actionService.getActions(null, null)).thenReturn(List.of(pending, completed));

        mockMvc.perform(get("/api/actions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("PENDING"))
                .andExpect(jsonPath("$.data[0].completedAt").doesNotExist())
                .andExpect(jsonPath("$.data[1].status").value("COMPLETED"))
                .andExpect(jsonPath("$.data[1].completedAt").exists());
    }

    @Test
    void listActions_withStatusFilter() throws Exception {
        when(actionService.getActions(ActionStatus.PENDING, null)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/actions").param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void listActions_withPriorityFilter() throws Exception {
        when(actionService.getActions(null, ActionPriority.HIGH)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/actions").param("priority", "HIGH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ── PATCH /api/actions/{id}/complete ───────────────────────────────────

    @Test
    void completeAction_success() throws Exception {
        UUID actionId = UUID.randomUUID();
        Action action = buildAction(actionId, "Pay rent", ActionStatus.COMPLETED,
                ActionPriority.HIGH, LocalDate.of(2026, 10, 1));
        action.setCompletedAt(Instant.parse("2026-09-18T12:00:00Z"));

        when(actionService.completeAction(actionId)).thenReturn(action);

        mockMvc.perform(patch("/api/actions/" + actionId + "/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(actionId.toString()))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.completedAt").exists());
    }

    @Test
    void completeAction_notFound_returns404() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        when(actionService.completeAction(nonExistentId))
                .thenThrow(new ActionNotFoundException("Action not found"));

        mockMvc.perform(patch("/api/actions/" + nonExistentId + "/complete"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Action not found"));
    }

    @Test
    void completeAction_invalidUuid_returns400() throws Exception {
        mockMvc.perform(patch("/api/actions/not-a-uuid/complete"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Invalid parameter: id"));
    }

    @Test
    void completeAction_alreadyCompleted_returnsExistingState() throws Exception {
        UUID actionId = UUID.randomUUID();
        Instant originalCompletedAt = Instant.parse("2026-09-15T10:00:00Z");
        Action action = buildAction(actionId, "Already done", ActionStatus.COMPLETED,
                ActionPriority.MEDIUM, null);
        action.setCompletedAt(originalCompletedAt);

        when(actionService.completeAction(actionId)).thenReturn(action);

        mockMvc.perform(patch("/api/actions/" + actionId + "/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.completedAt").value("2026-09-15T10:00:00Z"));
    }
}
