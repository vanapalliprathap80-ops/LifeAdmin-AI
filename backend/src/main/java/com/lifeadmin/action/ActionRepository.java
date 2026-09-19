package com.lifeadmin.action;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ActionRepository extends JpaRepository<Action, UUID> {

    List<Action> findAllByUserIdOrderByDeadlineAsc(UUID userId);

    List<Action> findByUserIdAndDocumentId(UUID userId, UUID documentId);

    List<Action> findByUserIdAndStatusOrderByDeadlineAsc(UUID userId, ActionStatus status);

    List<Action> findByDocumentIdAndStatusNot(UUID documentId, ActionStatus status);

    List<Action> findByDocumentIdAndObligationIdAndStatus(UUID documentId, UUID obligationId, ActionStatus status);

    List<Action> findByDocumentIdAndStatus(UUID documentId, ActionStatus status);
}
