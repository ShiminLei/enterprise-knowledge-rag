package com.aishare.knowledgerag.evaluation;

import com.aishare.knowledgerag.answer.AnswerCitation;
import com.aishare.knowledgerag.answer.GroundedAnswer;
import com.aishare.knowledgerag.answer.RagAnswerService;
import com.aishare.knowledgerag.retrieval.VectorSearchQuery;
import com.aishare.knowledgerag.security.AccessContext;
import com.aishare.knowledgerag.security.PermissionLevel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnswerEvaluationServiceTest {

    @Test
    void passesGroundedAnswerWithKeywordsAndExpectedCitation() {
        RagAnswerService answerService = mock(RagAnswerService.class);
        EvaluationRepository repository = mock(EvaluationRepository.class);
        UUID documentId = UUID.randomUUID();
        when(repository.findCases()).thenReturn(List.of(
                evaluationCase(
                        "vpn", true, List.of("IT-VPN-004"),
                        List.of("公司邮箱", "动态口令")
                ),
                evaluationCase("unknown", false, List.of(), List.of())
        ));
        when(repository.resolveDocumentIds(any(), any()))
                .thenReturn(Map.of("IT-VPN-004", documentId));
        when(answerService.answer(any(VectorSearchQuery.class)))
                .thenReturn(new GroundedAnswer(
                        "使用公司邮箱登录，并绑定动态口令。[1]",
                        true,
                        1,
                        List.of(citation(documentId)),
                        "v2"
                ))
                .thenReturn(new GroundedAnswer(
                        "根据当前可访问的企业知识库资料，无法找到足够依据回答这个问题。",
                        false,
                        0,
                        List.of(),
                        "none"
                ));
        AnswerEvaluationService service = new AnswerEvaluationService(
                answerService, repository
        );

        EvaluationRunReport report = service.run(accessContext(), 5, 0.35);

        assertThat(report.summary().passedCases()).isEqualTo(2);
        assertThat(report.summary().answerableHitRate()).isEqualTo(1.0);
        assertThat(report.results().get(0).metrics())
                .containsEntry("keywordCoverage", 1.0)
                .containsEntry("allReferencedCitationsValid", true)
                .containsEntry("expectedDocumentCited", true)
                .containsEntry("promptVersion", "v2");
        assertThat(report.results().get(0).answer())
                .contains("公司邮箱", "动态口令", "[1]");
    }

    @Test
    void failsWhenRequiredKeywordAndCitationAreMissing() {
        RagAnswerService answerService = mock(RagAnswerService.class);
        EvaluationRepository repository = mock(EvaluationRepository.class);
        UUID documentId = UUID.randomUUID();
        when(repository.findCases()).thenReturn(List.of(
                evaluationCase(
                        "vpn", true, List.of("IT-VPN-004"),
                        List.of("公司邮箱", "动态口令")
                )
        ));
        when(repository.resolveDocumentIds(any(), any()))
                .thenReturn(Map.of("IT-VPN-004", documentId));
        when(answerService.answer(any(VectorSearchQuery.class)))
                .thenReturn(new GroundedAnswer(
                        "使用公司邮箱登录。",
                        true,
                        1,
                        List.of(citation(documentId)),
                        "v2"
                ));
        AnswerEvaluationService service = new AnswerEvaluationService(
                answerService, repository
        );

        EvaluationCaseResult result = service.run(accessContext(), 5, 0.35)
                .results().get(0);

        assertThat(result.passed()).isFalse();
        assertThat(result.metrics().get("missingExpectedKeywords"))
                .isEqualTo(List.of("动态口令"));
        assertThat(result.metrics()).containsEntry("hasCitation", false);
        assertThat(result.metrics()).containsEntry("expectedDocumentCited", false);
    }

    private EvaluationCase evaluationCase(
            String key,
            boolean shouldAnswer,
            List<String> expectedDocuments,
            List<String> expectedKeywords
    ) {
        return new EvaluationCase(
                UUID.randomUUID(), key, key + " question",
                expectedDocuments, expectedKeywords, shouldAnswer,
                PermissionLevel.INTERNAL, List.of("answer")
        );
    }

    private AnswerCitation citation(UUID documentId) {
        return new AnswerCitation(
                1, UUID.randomUUID(), documentId,
                "VPN 手册", 1, "IT 服务台", "2.3"
        );
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
