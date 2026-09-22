package com.aishare.knowledgerag.evaluation;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record EvaluationRunRecord(
        UUID runId,
        String status,
        Map<String, Object> configuration,
        EvaluationRunSummary summary,
        Instant startedAt,
        Instant completedAt
) {
    public EvaluationRunRecord {
        configuration = Map.copyOf(configuration);
    }

    public String type() {
        Object value = configuration.get("type");
        return value == null ? "UNKNOWN" : value.toString();
    }
}
