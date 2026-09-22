package com.aishare.knowledgerag.retrieval;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rag.retrieval")
public record RetrievalProperties(
        int vectorTopK,
        int keywordTopK,
        int finalTopK,
        double minScore,
        int rrfK
) {
    public RetrievalProperties {
        if (vectorTopK <= 0 || keywordTopK <= 0 || finalTopK <= 0 || rrfK <= 0) {
            throw new IllegalArgumentException("检索数量配置必须大于 0");
        }
        if (minScore < 0 || minScore > 1) {
            throw new IllegalArgumentException("rag.retrieval.min-score 必须在 0 到 1 之间");
        }
    }
}
