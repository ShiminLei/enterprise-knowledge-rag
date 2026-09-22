package com.aishare.knowledgerag.api.dto;

import com.aishare.knowledgerag.document.DocumentCategory;
import com.aishare.knowledgerag.retrieval.RetrievalProperties;
import com.aishare.knowledgerag.retrieval.VectorSearchQuery;
import com.aishare.knowledgerag.security.AccessContext;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record VectorSearchRequest(
        @NotBlank String question,
        @NotNull UUID tenantId,
        @NotBlank String userId,
        DocumentCategory category,
        @Min(1) @Max(50) Integer topK,
        @DecimalMin("0.0") @DecimalMax("1.0") Double minScore
) {
    public VectorSearchQuery toQuery(RetrievalProperties defaults, AccessContext accessContext) {
        return new VectorSearchQuery(
                question,
                accessContext,
                category,
                topK == null ? defaults.vectorTopK() : topK,
                minScore == null ? defaults.minScore() : minScore
        );
    }
}
