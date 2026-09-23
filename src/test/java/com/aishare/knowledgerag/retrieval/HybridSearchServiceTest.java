package com.aishare.knowledgerag.retrieval;

import com.aishare.knowledgerag.document.DocumentCategory;
import com.aishare.knowledgerag.security.AccessContext;
import com.aishare.knowledgerag.security.PermissionLevel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HybridSearchServiceTest {

    @Test
    void fusesAndDeduplicatesResultsUsingReciprocalRankFusion() {
        RetrievedChunk vectorFirst = chunk("10000000-0000-0000-0000-000000000001", "语义结果", 0.91);
        RetrievedChunk both = chunk("10000000-0000-0000-0000-000000000002", "两路命中", 0.82);
        RetrievedChunk keywordOnly = chunk("10000000-0000-0000-0000-000000000003", "关键词结果", 0.76);

        VectorSearchService vectorService = mock(VectorSearchService.class);
        when(vectorService.search(any())).thenReturn(List.of(vectorFirst, both));
        KeywordSearchRepository keywordRepository = query -> List.of(both, keywordOnly);
        HybridSearchService service = new HybridSearchService(
                vectorService,
                keywordRepository,
                new RetrievalProperties(20, 20, 5, 0.35, 60),
                new HybridResultReranker()
        );

        List<HybridSearchResult> results = service.search(query(5));

        assertThat(results).extracting(result -> result.chunk().content())
                .containsExactly("两路命中", "语义结果", "关键词结果");
        HybridSearchResult first = results.get(0);
        assertThat(first.vectorRank()).isEqualTo(2);
        assertThat(first.keywordRank()).isEqualTo(1);
        assertThat(first.rrfScore()).isEqualTo(1.0 / 62 + 1.0 / 61);
    }

    @Test
    void limitsFinalResultsWithoutReducingCandidateRecall() {
        RetrievedChunk first = chunk("10000000-0000-0000-0000-000000000001", "第一条", 0.9);
        RetrievedChunk second = chunk("10000000-0000-0000-0000-000000000002", "第二条", 0.8);
        VectorSearchService vectorService = mock(VectorSearchService.class);
        when(vectorService.search(any())).thenReturn(List.of(first, second));
        KeywordSearchRepository keywordRepository = query -> List.of();
        HybridSearchService service = new HybridSearchService(
                vectorService,
                keywordRepository,
                new RetrievalProperties(20, 20, 5, 0.35, 60),
                new HybridResultReranker()
        );

        assertThat(service.search(query(1))).hasSize(1);
    }

    private VectorSearchQuery query(int topK) {
        return new VectorSearchQuery(
                "VPN 错误码 720",
                new AccessContext(
                        UUID.fromString("00000000-0000-0000-0000-000000000001"),
                        "zhangsan",
                        Set.of("信息技术部"),
                        PermissionLevel.INTERNAL
                ),
                null,
                topK,
                0.35
        );
    }

    private RetrievedChunk chunk(String id, String content, double score) {
        return new RetrievedChunk(
                UUID.fromString(id),
                UUID.fromString("20000000-0000-0000-0000-000000000001"),
                0,
                content,
                "VPN 手册",
                null,
                DocumentCategory.MANUAL,
                "1.0",
                "IT 服务台",
                score
        );
    }
}
