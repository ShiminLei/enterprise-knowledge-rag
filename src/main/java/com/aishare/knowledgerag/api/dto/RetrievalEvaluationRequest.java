package com.aishare.knowledgerag.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record RetrievalEvaluationRequest(
        @Min(1) @Max(50) Integer topK,
        @DecimalMin("0.0") @DecimalMax("1.0") Double minScore
) {
    public int resolvedTopK(int defaultValue) {
        return topK == null ? defaultValue : topK;
    }

    public double resolvedMinScore(double defaultValue) {
        return minScore == null ? defaultValue : minScore;
    }
}
