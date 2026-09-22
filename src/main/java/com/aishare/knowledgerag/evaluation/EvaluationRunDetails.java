package com.aishare.knowledgerag.evaluation;

import java.util.List;
import java.util.Set;

public record EvaluationRunDetails(
        EvaluationRunRecord run,
        Set<String> promptVersions,
        List<EvaluationCaseResult> results
) {
    public EvaluationRunDetails {
        promptVersions = Set.copyOf(promptVersions);
        results = List.copyOf(results);
    }
}
