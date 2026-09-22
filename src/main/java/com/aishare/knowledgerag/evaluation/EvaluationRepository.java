package com.aishare.knowledgerag.evaluation;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Optional;

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

    List<EvaluationRunRecord> findRuns(UUID tenantId, int limit);

    Optional<EvaluationRunRecord> findRun(UUID tenantId, UUID runId);

    List<EvaluationCaseResult> findResults(UUID runId);
}
