package com.aishare.knowledgerag.evaluation;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record EvaluationCaseResult(
        UUID id,
        UUID runId,
        UUID caseId,
        String caseKey,
        String question,
        boolean passed,
        List<EvaluationRetrievedChunk> retrievedChunks,
        Map<String, Object> metrics,
        long latencyMs,
        String errorMessage
) {
    public EvaluationCaseResult {
        retrievedChunks = List.copyOf(retrievedChunks);
        metrics = Map.copyOf(metrics);
    }
}
