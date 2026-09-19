package com.lifeadmin.action;

import com.lifeadmin.ai.dto.AiAnalysisResponse;
import com.lifeadmin.ai.dto.AiDateRelationshipDto;
import com.lifeadmin.deadline.DeadlineCalculator;
import com.lifeadmin.deadline.DeadlineResult;
import com.lifeadmin.document.Document;
import com.lifeadmin.document.KeyDate;
import com.lifeadmin.document.KeyDateRepository;
import com.lifeadmin.obligation.Obligation;
import com.lifeadmin.obligation.ObligationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ActionGenerationService {

    private static final Logger log = LoggerFactory.getLogger(ActionGenerationService.class);

    private final ActionRepository actionRepository;
    private final ObligationRepository obligationRepository;
    private final KeyDateRepository keyDateRepository;
    private final DeadlineCalculator deadlineCalculator;
    private final ActionFactory actionFactory;

    public ActionGenerationService(ActionRepository actionRepository,
                                   ObligationRepository obligationRepository,
                                   KeyDateRepository keyDateRepository,
                                   DeadlineCalculator deadlineCalculator,
                                   ActionFactory actionFactory) {
        this.actionRepository = actionRepository;
        this.obligationRepository = obligationRepository;
        this.keyDateRepository = keyDateRepository;
        this.deadlineCalculator = deadlineCalculator;
        this.actionFactory = actionFactory;
    }

    /**
     * Generates Actions for a given document based on its extracted obligations and the AI response.
     * Uses REQUIRES_NEW so that action generation failure does not roll back the AI extraction persistence.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void generateActions(Document document, AiAnalysisResponse aiResponse) {
        UUID documentId = document.getId();
        log.info("Generating actions for document {}", documentId);

        // 1. Cleanup non-completed actions for this document (safe reprocessing)
        deleteNonCompletedActions(documentId);

        // 2. Load necessary data
        List<Obligation> obligations = obligationRepository.findByDocumentId(documentId);
        List<KeyDate> keyDates = keyDateRepository.findByDocumentId(documentId);
        List<Action> completedActions = actionRepository.findByDocumentIdAndStatus(documentId, ActionStatus.COMPLETED);

        // 3. Process each obligation
        for (Obligation obligation : obligations) {
            try {
                // Find matching DTO to get relationship mapping
                var aiObligationOpt = aiResponse.getObligations().stream()
                        .filter(dto -> dto.getType().equals(obligation.getObligationType())
                                && dto.getDescription().equals(obligation.getDescription()))
                        .findFirst();

                DeadlineResult deadlineResult = null;

                if (aiObligationOpt.isPresent() && aiObligationOpt.get().getRelatedDateType() != null) {
                    String relatedDateType = aiObligationOpt.get().getRelatedDateType();
                    
                    // See if it's a relationship (e.g. NOTICE_PERIOD_BEFORE_EXPIRY)
                    Optional<AiDateRelationshipDto> relationshipOpt = aiResponse.getDateRelationships().stream()
                            .filter(rel -> rel.getType().equals(relatedDateType))
                            .findFirst();

                    if (relationshipOpt.isPresent()) {
                        AiDateRelationshipDto rel = relationshipOpt.get();
                        deadlineResult = deadlineCalculator.calculate(
                                rel.getType(),
                                rel.getAnchorDateType(),
                                rel.getOffsetDays(),
                                rel.getComputedDate(),
                                keyDates
                        );
                    } else {
                        // It's a direct reference to a KeyDateType (e.g., EXPIRY_DATE)
                        Optional<LocalDate> directDate = deadlineCalculator.resolveDirectDate(relatedDateType, keyDates);
                        if (directDate.isPresent()) {
                            deadlineResult = DeadlineResult.success(directDate.get());
                        } else {
                            deadlineResult = DeadlineResult.unresolved("Related date '" + relatedDateType + "' not found in KeyDates");
                        }
                    }
                }

                Action action = actionFactory.createAction(document, obligation, deadlineResult);

                // Deduplication check: if we already have an active action for this obligation (e.g. completed), skip
                // We match by Title because old Obligations might have been deleted/recreated during reprocessing,
                // causing their IDs to change and old Actions to have obligation_id = NULL.
                java.util.Optional<Action> existingOpt = completedActions.stream()
                        .filter(a -> a.getTitle().equals(action.getTitle()))
                        .findFirst();
                
                if (existingOpt.isPresent()) {
                    Action existing = existingOpt.get();
                    // Self-heal: link the new obligation to the existing completed action
                    if (existing.getObligation() == null || !existing.getObligation().getId().equals(obligation.getId())) {
                        existing.setObligation(obligation);
                        actionRepository.save(existing);
                        log.info("Self-healed completed action '{}' by linking to new obligation {}", existing.getTitle(), obligation.getId());
                    } else {
                        log.info("Skipping action generation for obligation {} - already completed action exists", obligation.getId());
                    }
                    continue;
                }

                actionRepository.save(action);
                log.info("Generated action '{}' with status {} and deadline {}",
                        action.getTitle(), action.getStatus(), action.getDeadline());

            } catch (Exception e) {
                // Failure isolation: log error but continue processing other obligations
                log.error("Failed to generate action for obligation {}: {}", obligation.getId(), e.getMessage(), e);
            }
        }
    }

    private void deleteNonCompletedActions(UUID documentId) {
        List<Action> actionsToDelete = actionRepository.findByDocumentIdAndStatusNot(documentId, ActionStatus.COMPLETED);
        if (!actionsToDelete.isEmpty()) {
            actionRepository.deleteAll(actionsToDelete);
            log.info("Deleted {} non-completed actions for document {}", actionsToDelete.size(), documentId);
        }
    }
}
