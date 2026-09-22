package com.aishare.knowledgerag.retrieval;

import com.aishare.knowledgerag.embedding.EmbeddingGateway;
import com.aishare.knowledgerag.embedding.EmbeddingGenerationException;
import com.aishare.knowledgerag.embedding.EmbeddingProperties;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VectorSearchService {

    private final EmbeddingGateway embeddingGateway;
    private final EmbeddingProperties embeddingProperties;
    private final VectorSearchRepository searchRepository;

    public VectorSearchService(
            EmbeddingGateway embeddingGateway,
            EmbeddingProperties embeddingProperties,
            VectorSearchRepository searchRepository
    ) {
        this.embeddingGateway = embeddingGateway;
        this.embeddingProperties = embeddingProperties;
        this.searchRepository = searchRepository;
    }

    public List<RetrievedChunk> search(VectorSearchQuery query) {
        List<float[]> vectors = embeddingGateway.embed(List.of(query.question()));
        if (vectors == null || vectors.size() != 1) {
            throw new EmbeddingGenerationException("查询 Embedding 返回数量不正确");
        }
        float[] queryEmbedding = vectors.get(0);
        validateEmbedding(queryEmbedding);
        return searchRepository.search(query, queryEmbedding);
    }

    private void validateEmbedding(float[] embedding) {
        if (embedding == null || embedding.length != embeddingProperties.dimensions()) {
            throw new EmbeddingGenerationException(
                    "查询 Embedding 维度不正确：期望 %d，实际 %s"
                            .formatted(
                                    embeddingProperties.dimensions(),
                                    embedding == null ? "null" : embedding.length
                            )
            );
        }
        for (float value : embedding) {
            if (!Float.isFinite(value)) {
                throw new EmbeddingGenerationException("查询 Embedding 包含非有限数值");
            }
        }
    }
}
