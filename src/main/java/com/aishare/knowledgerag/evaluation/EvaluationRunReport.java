package com.aishare.knowledgerag.evaluation;

import java.util.List;
import java.util.UUID;

public record EvaluationRunReport(
        UUID runId,
        String status,
        EvaluationRunSummary summary,
        List<EvaluationCaseResult> results
) {
    public EvaluationRunReport {
        results = List.copyOf(results);
    }
}
