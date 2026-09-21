package com.aishare.knowledgerag.chunking;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rag.chunk")
public record ChunkingProperties(int size, int overlap) {

    public ChunkingProperties {
        if (size < 100) {
            throw new IllegalArgumentException("rag.chunk.size 不能小于 100");
        }
        if (overlap < 0) {
            throw new IllegalArgumentException("rag.chunk.overlap 不能小于 0");
        }
        if (overlap >= size) {
            throw new IllegalArgumentException("rag.chunk.overlap 必须小于 rag.chunk.size");
        }
    }
}
