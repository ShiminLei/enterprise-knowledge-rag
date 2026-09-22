package com.aishare.knowledgerag.retrieval;

import com.aishare.knowledgerag.document.DocumentCategory;
import com.aishare.knowledgerag.security.AccessContext;

import java.util.Objects;

public record VectorSearchQuery(
        String question,
        AccessContext accessContext,
        DocumentCategory category,
        int topK,
        double minScore
) {
    public VectorSearchQuery {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("检索问题不能为空");
        }
        question = question.strip();
        Objects.requireNonNull(accessContext, "访问上下文不能为空");
        if (topK <= 0 || topK > 50) {
            throw new IllegalArgumentException("topK 必须在 1 到 50 之间");
        }
        if (minScore < 0 || minScore > 1) {
            throw new IllegalArgumentException("minScore 必须在 0 到 1 之间");
        }
    }
}
