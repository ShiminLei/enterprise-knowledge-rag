package com.aishare.knowledgerag.evaluation;

import com.aishare.knowledgerag.document.DocumentCategory;
import com.aishare.knowledgerag.retrieval.HybridSearchResult;
import com.aishare.knowledgerag.retrieval.HybridSearchService;
import com.aishare.knowledgerag.retrieval.RetrievedChunk;
import com.aishare.knowledgerag.security.AccessContext;
import com.aishare.knowledgerag.security.PermissionLevel;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RetrievalEvaluationServiceTest {

    @Test
    void evaluatesExpectedHitAndCorrectRefusal() {
        HybridSearchService searchService = mock(HybridSearchService.class);
        EvaluationRepository repository = mock(EvaluationRepository.class);
        UUID expectedDocumentId = UUID.randomUUID();
        when(repository.findCases()).thenReturn(List.of(
                evaluationCase("vpn", true, List.of("IT-VPN-004")),
                evaluationCase("unknown", false, List.of())
        ));
        when(repository.resolveDocumentIds(any(), any()))
                .thenReturn(Map.of("IT-VPN-004", expectedDocumentId));
        when(searchService.search(any()))
                .thenReturn(List.of(searchResult(expectedDocumentId)))
                .thenReturn(List.of());
        RetrievalEvaluationService service = new RetrievalEvaluationService(
                searchService, repository
        );

        EvaluationRunReport report = service.run(accessContext(), 5, 0.35);

        assertThat(report.status()).isEqualTo("COMPLETED");
        assertThat(report.summary().totalCases()).isEqualTo(2);
        assertThat(report.summary().passedCases()).isEqualTo(2);
        assertThat(report.summary().passRate()).isEqualTo(1.0);
        assertThat(report.summary().decisionAccuracy()).isEqualTo(1.0);
        assertThat(report.summary().answerableHitRate()).isEqualTo(1.0);
        assertThat(report.results().get(0).metrics())
                .containsEntry("hitAtK", true)
                .containsEntry("documentRecall", 1.0);
        verify(repository).startRun(any(), anyMap(), any());
        verify(repository).completeRun(any(),
                org.mockito.ArgumentMatchers.eq("COMPLETED"), any(), any());
    }

    @Test
    void recordsOneCaseErrorAndContinuesTheRun() {
        HybridSearchService searchService = mock(HybridSearchService.class);
        EvaluationRepository repository = mock(EvaluationRepository.class);
        when(repository.findCases()).thenReturn(List.of(
                evaluationCase("broken", true, List.of("IT-VPN-004")),
                evaluationCase("unknown", false, List.of())
        ));
        when(repository.resolveDocumentIds(any(), any())).thenReturn(Map.of());
        when(searchService.search(any()))
                .thenThrow(new IllegalStateException("embedding unavailable"))
                .thenReturn(List.of());
        RetrievalEvaluationService service = new RetrievalEvaluationService(
                searchService, repository
        );

        EvaluationRunReport report = service.run(accessContext(), 5, 0.35);

        assertThat(report.status()).isEqualTo("COMPLETED_WITH_ERRORS");
        assertThat(report.summary().errorCases()).isEqualTo(1);
        assertThat(report.results()).hasSize(2);
        assertThat(report.results().get(0).errorMessage())
                .isEqualTo("embedding unavailable");
        ArgumentCaptor<EvaluationCaseResult> saved =
                ArgumentCaptor.forClass(EvaluationCaseResult.class);
        verify(repository, org.mockito.Mockito.times(2)).saveResult(saved.capture());
        assertThat(saved.getAllValues()).hasSize(2);
    }

    private EvaluationCase evaluationCase(
            String key,
            boolean shouldAnswer,
            List<String> expectedDocuments
    ) {
        return new EvaluationCase(
                UUID.randomUUID(), key, key + " question", expectedDocuments,
                List.of(), shouldAnswer, PermissionLevel.INTERNAL, List.of("retrieval")
        );
    }

    private HybridSearchResult searchResult(UUID documentId) {
        RetrievedChunk chunk = new RetrievedChunk(
                UUID.randomUUID(), documentId, 0, "内容", "标题", 1,
                DocumentCategory.MANUAL, "1.0", "source", 0.9
        );
        return new HybridSearchResult(chunk, 1, 1, 0.9, 0.8, 0.03);
    }

    private AccessContext accessContext() {
        return new AccessContext(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "zhangsan",
                Set.of("信息技术部"),
                PermissionLevel.INTERNAL
        );
    }
}
