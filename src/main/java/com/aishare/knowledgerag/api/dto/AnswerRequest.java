package com.aishare.knowledgerag.api.dto;

import com.aishare.knowledgerag.document.DocumentCategory;
import com.aishare.knowledgerag.retrieval.RetrievalProperties;
import com.aishare.knowledgerag.retrieval.VectorSearchQuery;
import com.aishare.knowledgerag.security.AccessContext;
import com.aishare.knowledgerag.security.PermissionLevel;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Set;
import java.util.UUID;

public record AnswerRequest(
        UUID conversationId,
        @NotBlank String question,
        @NotNull UUID tenantId,
        @NotBlank String userId,
        @NotNull Set<@NotBlank String> departments,
        @NotNull PermissionLevel permissionLevel,
        DocumentCategory category,
        @Min(1) @Max(20) Integer contextTopK,
        @DecimalMin("0.0") @DecimalMax("1.0") Double minVectorScore
) {
    public VectorSearchQuery toQuery(RetrievalProperties defaults) {
        return new VectorSearchQuery(
                question,
                new AccessContext(tenantId, userId, Set.copyOf(departments), permissionLevel),
                category,
                contextTopK == null ? defaults.finalTopK() : contextTopK,
                minVectorScore == null ? defaults.minScore() : minVectorScore
        );
    }
}
