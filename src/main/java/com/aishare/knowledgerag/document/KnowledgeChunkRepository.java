package com.aishare.knowledgerag.document;

import java.util.List;

public interface KnowledgeChunkRepository {

    void insertAll(List<KnowledgeChunk> chunks);
}
