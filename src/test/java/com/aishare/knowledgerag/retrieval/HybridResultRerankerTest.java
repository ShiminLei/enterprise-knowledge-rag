package com.aishare.knowledgerag.retrieval;

import com.aishare.knowledgerag.document.DocumentCategory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class HybridResultRerankerTest {

    @Test
    void promotesEvidenceConfirmedByBothRetrievalChannels() {
        HybridSearchResult vectorOnly = result("10000000-0000-0000-0000-000000000001", 1, null);
        HybridSearchResult dual = result("10000000-0000-0000-0000-000000000002", 2, 1);

        List<HybridSearchResult> reranked = new HybridResultReranker()
                .rerank(List.of(vectorOnly, dual));

        assertThat(reranked).extracting(item -> item.chunk().chunkId())
                .containsExactly(dual.chunk().chunkId(), vectorOnly.chunk().chunkId());
        assertThat(reranked.get(0).rerankScore())
                .isGreaterThan(reranked.get(0).rrfScore());
    }

    private HybridSearchResult result(String id, Integer vectorRank, Integer keywordRank) {
        RetrievedChunk chunk = new RetrievedChunk(
                UUID.fromString(id), UUID.randomUUID(), 0, "内容", "标题", null,
                DocumentCategory.MANUAL, "1.0", "测试", 0.8
        );
        return new HybridSearchResult(
                chunk, vectorRank, keywordRank,
                vectorRank == null ? null : 0.8,
                keywordRank == null ? null : 0.8,
                0.02
        );
    }
}
