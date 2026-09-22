package com.aishare.knowledgerag.evaluation;

import java.util.Set;
import java.util.UUID;

public record EvaluationRunComparison(
        UUID baselineRunId,
        UUID candidateRunId,
        String evaluationType,
        Set<String> baselinePromptVersions,
        Set<String> candidatePromptVersions,
        EvaluationMetricComparison passRate,
        EvaluationMetricComparison decisionAccuracy,
        EvaluationMetricComparison answerableHitRate
) {
    public EvaluationRunComparison {
        baselinePromptVersions = Set.copyOf(baselinePromptVersions);
        candidatePromptVersions = Set.copyOf(candidatePromptVersions);
    }
}
