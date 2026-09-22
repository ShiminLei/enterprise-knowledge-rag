package com.aishare.knowledgerag.retrieval;

import com.aishare.knowledgerag.embedding.EmbeddingGateway;
import com.aishare.knowledgerag.embedding.EmbeddingGenerationException;
import com.aishare.knowledgerag.embedding.EmbeddingProperties;
import com.aishare.knowledgerag.security.AccessContext;
import com.aishare.knowledgerag.security.PermissionLevel;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VectorSearchServiceTest {

    @Test
    void embedsQuestionThenSearchesRepository() {
        RecordingRepository repository = new RecordingRepository();
        EmbeddingGateway gateway = texts -> {
            assertThat(texts).containsExactly("如何登录 VPN？");
            return List.of(new float[]{0.1f, 0.2f, 0.3f, 0.4f});
        };
        VectorSearchService service = new VectorSearchService(
                gateway,
                new EmbeddingProperties(4, 32),
                repository
        );
        VectorSearchQuery query = query();

        List<RetrievedChunk> results = service.search(query);

        assertThat(results).isEmpty();
        assertThat(repository.query).isEqualTo(query);
        assertThat(repository.embedding).containsExactly(0.1f, 0.2f, 0.3f, 0.4f);
    }

    @Test
    void rejectsWrongQueryEmbeddingDimensionBeforeSearch() {
        RecordingRepository repository = new RecordingRepository();
        EmbeddingGateway gateway = texts -> List.of(new float[3]);
        VectorSearchService service = new VectorSearchService(
                gateway,
                new EmbeddingProperties(4, 32),
                repository
        );

        assertThatThrownBy(() -> service.search(query()))
                .isInstanceOf(EmbeddingGenerationException.class)
                .hasMessageContaining("期望 4，实际 3");
        assertThat(repository.query).isNull();
    }

    private VectorSearchQuery query() {
        return new VectorSearchQuery(
                "如何登录 VPN？",
                new AccessContext(
                        UUID.fromString("00000000-0000-0000-0000-000000000001"),
                        "zhangsan",
                        Set.of("信息技术部"),
                        PermissionLevel.INTERNAL
                ),
                null,
                20,
                0.35
        );
    }

    private static class RecordingRepository implements VectorSearchRepository {

        private VectorSearchQuery query;
        private float[] embedding;

        @Override
        public List<RetrievedChunk> search(VectorSearchQuery query, float[] queryEmbedding) {
            this.query = query;
            this.embedding = queryEmbedding.clone();
            return new ArrayList<>();
        }
    }
}
