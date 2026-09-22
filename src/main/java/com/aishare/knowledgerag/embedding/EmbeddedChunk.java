package com.aishare.knowledgerag.embedding;

import com.aishare.knowledgerag.document.KnowledgeChunk;

import java.util.Objects;

public record EmbeddedChunk(KnowledgeChunk chunk, float[] embedding) {

    public EmbeddedChunk {
        Objects.requireNonNull(chunk, "chunk 不能为空");
        embedding = Objects.requireNonNull(embedding, "embedding 不能为空").clone();
    }

    @Override
    public float[] embedding() {
        return embedding.clone();
    }
}
