package com.aishare.knowledgerag.retrieval;

import com.aishare.knowledgerag.document.DocumentCategory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class Bm25ScorerTest {

    @Test
    void ranksRareMatchingTermsAheadOfUnrelatedChunks() {
        Bm25Scorer scorer = new Bm25Scorer();
        RetrievedChunk vpn = chunk("VPN 错误码 720 的处理方法", "企业 VPN 手册");
        RetrievedChunk travel = chunk("差旅住宿费报销标准", "差旅制度");

        List<RetrievedChunk> results = scorer.rank(
                "VPN 错误码 720", List.of(travel, vpn), 5, 0.5
        );

        assertThat(results).singleElement().satisfies(result -> {
            assertThat(result.content()).contains("720");
            assertThat(result.score()).isBetween(0.0, 1.0);
        });
    }

    @Test
    void rejectsDocumentThatOnlyMatchesACommonQuestionTerm() {
        RetrievedChunk generic = chunk("公司制度统一由行政部发布", "制度说明");

        assertThat(new Bm25Scorer().rank(
                "公司食堂每月餐补标准是多少", List.of(generic), 5, 0.5
        )).isEmpty();
    }

    @Test
    void createsChineseBigramsAndLatinTokens() {
        assertThat(new Bm25Scorer().tokens("VPN 错误码 720"))
                .contains("vpn", "错误", "误码", "720");
    }

    private RetrievedChunk chunk(String content, String title) {
        return new RetrievedChunk(
                UUID.randomUUID(), UUID.randomUUID(), 0, content, title, null,
                DocumentCategory.MANUAL, "1.0", "测试", 0
        );
    }
}
