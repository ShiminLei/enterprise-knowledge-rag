package com.aishare.knowledgerag.evaluation;

import java.util.List;

final class EvaluationSummaries {

    private EvaluationSummaries() {
    }

    static EvaluationRunSummary summarize(
            List<EvaluationCaseResult> results,
            String answerableHitMetric
    ) {
        int total = results.size();
        int errors = (int) results.stream()
                .filter(result -> result.errorMessage() != null)
                .count();
        int passed = (int) results.stream().filter(EvaluationCaseResult::passed).count();
        int completed = total - errors;
        long decisionCorrect = results.stream()
                .filter(result -> result.errorMessage() == null)
                .filter(result -> Boolean.TRUE.equals(result.metrics().get("decisionCorrect")))
                .count();
        List<EvaluationCaseResult> answerable = results.stream()
                .filter(result -> result.errorMessage() == null)
                .filter(result -> Boolean.TRUE.equals(
                        result.metrics().get("expectedShouldAnswer")))
                .toList();
        long answerableHits = answerable.stream()
                .filter(result -> Boolean.TRUE.equals(
                        result.metrics().get(answerableHitMetric)))
                .count();
        return new EvaluationRunSummary(
                total,
                passed,
                total - passed,
                errors,
                ratio(passed, total),
                ratio(decisionCorrect, completed),
                ratio(answerableHits, answerable.size())
        );
    }

    private static double ratio(long numerator, long denominator) {
        return denominator == 0 ? 0.0 : (double) numerator / denominator;
    }
}
