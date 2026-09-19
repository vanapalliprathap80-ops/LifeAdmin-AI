package com.lifeadmin.action;

import com.lifeadmin.ai.dto.AiAnalysisResponse;
import com.lifeadmin.ai.dto.AiDateRelationshipDto;
import com.lifeadmin.ai.dto.AiObligationDto;
import com.lifeadmin.deadline.DeadlineCalculator;
import com.lifeadmin.deadline.DeadlineResult;
import com.lifeadmin.document.Document;
import com.lifeadmin.document.KeyDate;
import com.lifeadmin.document.KeyDateRepository;
import com.lifeadmin.obligation.Obligation;
import com.lifeadmin.obligation.ObligationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ActionGenerationServiceTest {

    @Mock private ActionRepository actionRepository;
    @Mock private ObligationRepository obligationRepository;
    @Mock private KeyDateRepository keyDateRepository;
    @Mock private DeadlineCalculator deadlineCalculator;
    @Mock private ActionFactory actionFactory;

    private ActionGenerationService service;

    @Captor private ArgumentCaptor<Action> actionCaptor;

    private Document document;
    private UUID docId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new ActionGenerationService(
                actionRepository, obligationRepository, keyDateRepository,
                deadlineCalculator, actionFactory
        );

        document = new Document();
        document.setId(docId);
    }

    @Test
    void testGenerateActions_success() {
        // Setup existing data
        Obligation ob = new Obligation();
        ob.setId(UUID.randomUUID());
        ob.setDocument(document);
        ob.setObligationType("TEST_OBLIGATION");
        ob.setDescription("Do something");

        when(obligationRepository.findByDocumentId(docId)).thenReturn(List.of(ob));
        when(keyDateRepository.findByDocumentId(docId)).thenReturn(List.of());

        // Setup AI Response
        AiAnalysisResponse response = new AiAnalysisResponse();
        
        AiObligationDto obDto = new AiObligationDto();
        obDto.setType("TEST_OBLIGATION");
        obDto.setDescription("Do something");
        obDto.setRelatedDateType("NOTICE_RELATION");
        response.setObligations(List.of(obDto));
        
        AiDateRelationshipDto relDto = new AiDateRelationshipDto();
        relDto.setType("NOTICE_RELATION");
        relDto.setAnchorDateType("EXPIRY_DATE");
        relDto.setOffsetDays(30);
        response.setDateRelationships(List.of(relDto));

        // Mocks
        DeadlineResult result = DeadlineResult.success(LocalDate.of(2026, 10, 1));
        when(deadlineCalculator.calculate(eq("NOTICE_RELATION"), eq("EXPIRY_DATE"), eq(30), any(), any()))
                .thenReturn(result);
                
        Action createdAction = new Action();
        when(actionFactory.createAction(document, ob, result)).thenReturn(createdAction);

        // Deduplication check
        when(actionRepository.findByDocumentIdAndObligationIdAndStatus(docId, ob.getId(), ActionStatus.COMPLETED))
                .thenReturn(List.of());

        // Execution
        service.generateActions(document, response);

        // Verification
        verify(actionRepository, never()).deleteAll(any()); // cleanup step
        verify(actionRepository).save(createdAction);
    }

    @Test
    void testGenerateActions_skipsCompleted() {
        Obligation ob = new Obligation();
        ob.setId(UUID.randomUUID());
        ob.setObligationType("TEST");
        ob.setDescription("Test");

        when(obligationRepository.findByDocumentId(docId)).thenReturn(List.of(ob));
        when(keyDateRepository.findByDocumentId(docId)).thenReturn(List.of());
        
        AiAnalysisResponse response = new AiAnalysisResponse();
        AiObligationDto obDto = new AiObligationDto();
        obDto.setType("TEST");
        obDto.setDescription("Test");
        response.setObligations(List.of(obDto));

        // Simulate an already completed action exists
        Action existingAction = new Action();
        existingAction.setTitle("Test Action");
        existingAction.setStatus(ActionStatus.COMPLETED);
        
        when(actionRepository.findByDocumentIdAndStatus(docId, ActionStatus.COMPLETED))
                .thenReturn(List.of(existingAction));
        
        Action createdAction = new Action();
        createdAction.setTitle("Test Action"); // matches existing
        when(actionFactory.createAction(any(), any(), any())).thenReturn(createdAction);

        service.generateActions(document, response);

        // Save shouldn't be called for new action, but it MIGHT be called to self-heal
        // In our case obligation is null initially, so it will self-heal
        verify(actionRepository).save(existingAction);
    }

    @Test
    void testCleanupDeletesOnlyNonCompleted() {
        Action a1 = new Action();
        a1.setStatus(ActionStatus.PENDING);
        
        when(actionRepository.findByDocumentIdAndStatusNot(docId, ActionStatus.COMPLETED))
                .thenReturn(List.of(a1));

        AiAnalysisResponse response = new AiAnalysisResponse();
        response.setObligations(List.of()); // No obligations to process
        
        service.generateActions(document, response);
        
        verify(actionRepository).deleteAll(List.of(a1));
    }
}
