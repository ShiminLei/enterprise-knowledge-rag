package com.aishare.knowledgerag.evaluation;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface EvaluationRepository {

    List<EvaluationCase> findCases();

    Map<String, UUID> resolveDocumentIds(
            UUID tenantId,
            Collection<String> externalDocumentIds
    );

    void startRun(UUID runId, Map<String, Object> configuration, Instant startedAt);

    void saveResult(EvaluationCaseResult result);

    void completeRun(
            UUID runId,
            String status,
            EvaluationRunSummary summary,
            Instant completedAt
    );
}
