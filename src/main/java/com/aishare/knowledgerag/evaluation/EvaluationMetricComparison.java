package com.aishare.knowledgerag.evaluation;

public record EvaluationMetricComparison(
        double baseline,
        double candidate,
        double delta
) {
    public static EvaluationMetricComparison between(double baseline, double candidate) {
        return new EvaluationMetricComparison(baseline, candidate, candidate - baseline);
    }
}
