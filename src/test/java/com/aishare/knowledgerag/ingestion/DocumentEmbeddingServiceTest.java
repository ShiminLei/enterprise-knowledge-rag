package com.aishare.knowledgerag.ingestion;

import com.aishare.knowledgerag.document.DocumentCategory;
import com.aishare.knowledgerag.document.DocumentStatus;
import com.aishare.knowledgerag.document.KnowledgeChunk;
import com.aishare.knowledgerag.document.KnowledgeDocument;
import com.aishare.knowledgerag.embedding.EmbeddingGateway;
import com.aishare.knowledgerag.embedding.EmbeddingGenerationException;
import com.aishare.knowledgerag.embedding.EmbeddingProperties;
import com.aishare.knowledgerag.security.PermissionLevel;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentEmbeddingServiceTest {

    @Test
    void embedsChunksInConfiguredBatchesAndIncludesTitlePath() {
        RecordingGateway gateway = new RecordingGateway(4);
        DocumentEmbeddingService service = new DocumentEmbeddingService(
                gateway,
                new EmbeddingProperties(4, 2)
        );

        EmbeddedIngestion result = service.embed(preparedIngestion(3));

        assertThat(gateway.batches).hasSize(2);
        assertThat(gateway.batches.get(0)).containsExactly(
                "VPN 手册 > 登录\n第 1 段",
                "VPN 手册 > 登录\n第 2 段"
        );
        assertThat(gateway.batches.get(1)).containsExactly("VPN 手册 > 登录\n第 3 段");
        assertThat(result.chunks()).hasSize(3);
        assertThat(result.chunks().get(0).embedding()).containsExactly(1f, 1f, 1f, 1f);
    }

    @Test
    void rejectsWrongVectorDimensionsBeforeDatabaseWrite() {
        EmbeddingGateway gateway = texts -> texts.stream().map(ignored -> new float[3]).toList();
        DocumentEmbeddingService service = new DocumentEmbeddingService(
                gateway,
                new EmbeddingProperties(4, 10)
        );

        assertThatThrownBy(() -> service.embed(preparedIngestion(1)))
                .isInstanceOf(EmbeddingGenerationException.class)
                .hasMessageContaining("期望 4，实际 3");
    }

    @Test
    void rejectsMismatchedVectorCount() {
        EmbeddingGateway gateway = texts -> List.of();
        DocumentEmbeddingService service = new DocumentEmbeddingService(
                gateway,
                new EmbeddingProperties(4, 10)
        );

        assertThatThrownBy(() -> service.embed(preparedIngestion(1)))
                .isInstanceOf(EmbeddingGenerationException.class)
                .hasMessageContaining("期望 1，实际 0");
    }

    private PreparedIngestion preparedIngestion(int chunkCount) {
        UUID documentId = UUID.fromString("10000000-0000-0000-0000-000000000001");
        UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Instant updatedAt = Instant.parse("2026-05-20T00:00:00Z");
        KnowledgeDocument document = new KnowledgeDocument(
                documentId,
                tenantId,
                "IT-VPN-004",
                "VPN 手册",
                "IT 服务台",
                "vpn.md",
                "text/markdown",
                DocumentCategory.MANUAL,
                "2.3",
                updatedAt,
                PermissionLevel.INTERNAL,
                "信息技术部",
                DocumentStatus.PENDING,
                "checksum"
        );
        List<KnowledgeChunk> chunks = new ArrayList<>();
        for (int index = 0; index < chunkCount; index++) {
            chunks.add(new KnowledgeChunk(
                    UUID.randomUUID(),
                    documentId,
                    tenantId,
                    index,
                    "第 " + (index + 1) + " 段",
                    "VPN 手册 > 登录",
                    null,
                    index + 1,
                    index + 1,
                    DocumentCategory.MANUAL,
                    "2.3",
                    updatedAt,
                    PermissionLevel.INTERNAL,
                    "信息技术部",
                    "IT 服务台"
            ));
        }
        return new PreparedIngestion(document, "VPN 手册", chunkCount, chunks, List.of());
    }

    private static class RecordingGateway implements EmbeddingGateway {

        private final int dimensions;
        private final List<List<String>> batches = new ArrayList<>();

        private RecordingGateway(int dimensions) {
            this.dimensions = dimensions;
        }

        @Override
        public List<float[]> embed(List<String> texts) {
            batches.add(List.copyOf(texts));
            return texts.stream().map(ignored -> {
                float[] vector = new float[dimensions];
                java.util.Arrays.fill(vector, 1f);
                return vector;
            }).toList();
        }
    }
}
