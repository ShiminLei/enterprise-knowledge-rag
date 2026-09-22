package com.aishare.knowledgerag.document;

import com.aishare.knowledgerag.embedding.EmbeddedChunk;

import java.util.List;

public interface KnowledgeChunkRepository {

    void insertAll(List<EmbeddedChunk> chunks);
}
