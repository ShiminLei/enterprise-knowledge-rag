package com.aishare.knowledgerag.ingestion;

import com.aishare.knowledgerag.document.KnowledgeChunk;
import com.aishare.knowledgerag.embedding.EmbeddedChunk;
import com.aishare.knowledgerag.embedding.EmbeddingGateway;
import com.aishare.knowledgerag.embedding.EmbeddingGenerationException;
import com.aishare.knowledgerag.embedding.EmbeddingProperties;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DocumentEmbeddingService {

    private final EmbeddingGateway embeddingGateway;
    private final EmbeddingProperties properties;

    public DocumentEmbeddingService(
            EmbeddingGateway embeddingGateway,
            EmbeddingProperties properties
    ) {
        this.embeddingGateway = embeddingGateway;
        this.properties = properties;
    }

    public EmbeddedIngestion embed(PreparedIngestion prepared) {
        List<EmbeddedChunk> embeddedChunks = new ArrayList<>(prepared.chunks().size());
        for (int start = 0; start < prepared.chunks().size(); start += properties.batchSize()) {
            int end = Math.min(start + properties.batchSize(), prepared.chunks().size());
            List<KnowledgeChunk> batch = prepared.chunks().subList(start, end);
            List<String> inputs = batch.stream().map(this::embeddingInput).toList();
            List<float[]> vectors = embeddingGateway.embed(inputs);
            validateBatch(vectors, batch.size());
            for (int index = 0; index < batch.size(); index++) {
                embeddedChunks.add(new EmbeddedChunk(batch.get(index), vectors.get(index)));
            }
        }
        return new EmbeddedIngestion(prepared, embeddedChunks);
    }

    private String embeddingInput(KnowledgeChunk chunk) {
        if (chunk.titlePath() == null || chunk.titlePath().isBlank()) {
            return chunk.content();
        }
        return chunk.titlePath() + "\n" + chunk.content();
    }

    private void validateBatch(List<float[]> vectors, int expectedCount) {
        if (vectors == null || vectors.size() != expectedCount) {
            throw new EmbeddingGenerationException(
                    "Embedding 返回数量不正确：期望 %d，实际 %s"
                            .formatted(expectedCount, vectors == null ? "null" : vectors.size())
            );
        }
        for (float[] vector : vectors) {
            if (vector == null || vector.length != properties.dimensions()) {
                throw new EmbeddingGenerationException(
                        "Embedding 维度不正确：期望 %d，实际 %s"
                                .formatted(properties.dimensions(), vector == null ? "null" : vector.length)
                );
            }
            for (float value : vector) {
                if (!Float.isFinite(value)) {
                    throw new EmbeddingGenerationException("Embedding 包含非有限数值");
                }
            }
        }
    }
}
