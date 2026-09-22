package com.aishare.knowledgerag.evaluation;

import com.aishare.knowledgerag.security.PermissionLevel;

import java.util.List;
import java.util.UUID;

public record EvaluationCase(
        UUID id,
        String caseKey,
        String question,
        List<String> expectedExternalDocumentIds,
        List<String> expectedKeywords,
        boolean shouldAnswer,
        PermissionLevel requiredPermissionLevel,
        List<String> tags
) {
    public EvaluationCase {
        expectedExternalDocumentIds = List.copyOf(expectedExternalDocumentIds);
        expectedKeywords = List.copyOf(expectedKeywords);
        tags = List.copyOf(tags);
    }
}
