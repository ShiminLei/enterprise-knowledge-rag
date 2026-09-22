package com.aishare.knowledgerag.retrieval;

import java.util.List;

public interface KeywordSearchRepository {

    List<RetrievedChunk> search(VectorSearchQuery query);
}
