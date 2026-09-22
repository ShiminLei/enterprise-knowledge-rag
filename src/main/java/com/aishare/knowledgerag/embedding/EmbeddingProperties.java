package com.aishare.knowledgerag.embedding;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rag.embedding")
public record EmbeddingProperties(int dimensions, int batchSize) {

    public EmbeddingProperties {
        if (dimensions <= 0) {
            throw new IllegalArgumentException("rag.embedding.dimensions 必须大于 0");
        }
        if (batchSize <= 0) {
            throw new IllegalArgumentException("rag.embedding.batch-size 必须大于 0");
        }
    }
}
