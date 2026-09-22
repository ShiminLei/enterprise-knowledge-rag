package com.aishare.knowledgerag.evaluation;

public record EvaluationRunSummary(
        int totalCases,
        int passedCases,
        int failedCases,
        int errorCases,
        double passRate,
        double decisionAccuracy,
        double answerableHitRate
) {
}
