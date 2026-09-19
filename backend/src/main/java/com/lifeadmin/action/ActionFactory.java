package com.lifeadmin.action;

import com.lifeadmin.deadline.DeadlineResult;
import com.lifeadmin.deadline.PriorityCalculator;
import com.lifeadmin.document.Document;
import com.lifeadmin.obligation.Obligation;
import org.springframework.stereotype.Component;

@Component
public class ActionFactory {

    private final PriorityCalculator priorityCalculator;

    public ActionFactory(PriorityCalculator priorityCalculator) {
        this.priorityCalculator = priorityCalculator;
    }

    /**
     * Creates a new Action entity from an Obligation and a resolved DeadlineResult.
     */
    public Action createAction(Document document, Obligation obligation, DeadlineResult deadlineResult) {
        Action action = new Action();
        action.setDocument(document);
        action.setUserId(document.getUserId());
        action.setObligation(obligation);
        
        // 1. Title & Description
        // For MVP, we use the obligation description as the title, or a formatted type
        String rawType = obligation.getObligationType();
        String title = formatTitle(rawType, obligation.getDescription());
        action.setTitle(title);
        action.setDescription(obligation.getDescription());
        
        // 2. Deadlines
        if (deadlineResult != null && deadlineResult.isResolved()) {
            action.setDeadline(deadlineResult.getCalculatedDate());
            // MVP: No undocumented product rules for advance buffers. Recommended date = deadline.
            action.setRecommendedDate(deadlineResult.getCalculatedDate());
        }

        // 3. Priority
        action.setPriority(priorityCalculator.calculate(action.getDeadline()));

        // 4. Status
        // If AI calculated something completely contradictory or unresolved relationships existed,
        // we might flag this for review. For MVP, if it was resolved, it's PENDING.
        // If we had a mismatch on a date relationship, flag for review.
        if (deadlineResult != null && deadlineResult.isAiMismatch()) {
            action.setStatus(ActionStatus.NEEDS_REVIEW);
            action.setReason("AI mismatch detected: " + deadlineResult.getValidationNote());
        } else if (deadlineResult != null && !deadlineResult.isResolved() && deadlineResult.getValidationNote() != null) {
             action.setStatus(ActionStatus.NEEDS_REVIEW);
             action.setReason("Could not calculate deadline safely: " + deadlineResult.getValidationNote());
        } else {
            action.setStatus(ActionStatus.PENDING);
            // Default reason explaining why this action exists
            if (deadlineResult != null && deadlineResult.isResolved()) {
                action.setReason(String.format("Calculated deadline %s from document dates.", deadlineResult.getCalculatedDate()));
            } else {
                action.setReason("Extracted from document obligation.");
            }
        }
        
        // 5. Evidence
        action.setEvidence(obligation.getEvidence());

        return action;
    }
    
    private String formatTitle(String obligationType, String description) {
        if (obligationType == null || obligationType.isBlank()) {
            // Fallback to truncating description for title
            if (description != null && description.length() > 50) {
                return description.substring(0, 47) + "...";
            }
            return description != null ? description : "Action required";
        }
        
        // Convert something like RENEW_POLICY to "Renew policy"
        String[] words = obligationType.split("_");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            String word = words[i].toLowerCase();
            if (i == 0) {
                sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
            } else {
                sb.append(" ").append(word);
            }
        }
        return sb.toString();
    }
}
