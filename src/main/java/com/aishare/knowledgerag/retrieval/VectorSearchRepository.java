package com.aishare.knowledgerag.retrieval;

import java.util.List;

public interface VectorSearchRepository {

    List<RetrievedChunk> search(VectorSearchQuery query, float[] queryEmbedding);
}
