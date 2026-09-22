package com.aishare.knowledgerag.evaluation;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EvaluationHistoryServiceTest {

    private static final UUID TENANT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void returnsDetailsWithActualPromptVersions() {
        EvaluationRepository repository = mock(EvaluationRepository.class);
        UUID runId = UUID.randomUUID();
        when(repository.findRun(TENANT_ID, runId))
                .thenReturn(Optional.of(run(runId, "RAG_ANSWER", 0.8)));
        when(repository.findResults(runId)).thenReturn(List.of(
                result(runId, "v1"),
                result(runId, "none")
        ));
        EvaluationHistoryService service = new EvaluationHistoryService(repository);

        EvaluationRunDetails details = service.details(TENANT_ID, runId);

        assertThat(details.promptVersions()).containsExactly("v1");
        assertThat(details.results()).hasSize(2);
    }

    @Test
    void comparesCandidateAgainstBaseline() {
        EvaluationRepository repository = mock(EvaluationRepository.class);
        UUID baselineId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();
        when(repository.findRun(TENANT_ID, baselineId))
                .thenReturn(Optional.of(run(baselineId, "RAG_ANSWER", 0.6)));
        when(repository.findRun(TENANT_ID, candidateId))
                .thenReturn(Optional.of(run(candidateId, "RAG_ANSWER", 0.9)));
        when(repository.findResults(baselineId))
                .thenReturn(List.of(result(baselineId, "v1")));
        when(repository.findResults(candidateId))
                .thenReturn(List.of(result(candidateId, "v2")));
        EvaluationHistoryService service = new EvaluationHistoryService(repository);

        EvaluationRunComparison comparison = service.compare(
                TENANT_ID, baselineId, candidateId
        );

        assertThat(comparison.baselinePromptVersions()).containsExactly("v1");
        assertThat(comparison.candidatePromptVersions()).containsExactly("v2");
        assertThat(comparison.passRate().baseline()).isEqualTo(0.6);
        assertThat(comparison.passRate().candidate()).isEqualTo(0.9);
        assertThat(comparison.passRate().delta()).isCloseTo(
                0.3, org.assertj.core.data.Offset.offset(0.000001)
        );
    }

    @Test
    void rejectsComparisonAcrossDifferentEvaluationTypes() {
        EvaluationRepository repository = mock(EvaluationRepository.class);
        UUID baselineId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();
        when(repository.findRun(TENANT_ID, baselineId))
                .thenReturn(Optional.of(run(baselineId, "HYBRID_RETRIEVAL", 0.6)));
        when(repository.findRun(TENANT_ID, candidateId))
                .thenReturn(Optional.of(run(candidateId, "RAG_ANSWER", 0.9)));
        when(repository.findResults(baselineId)).thenReturn(List.of());
        when(repository.findResults(candidateId)).thenReturn(List.of());
        EvaluationHistoryService service = new EvaluationHistoryService(repository);

        assertThatThrownBy(() -> service.compare(TENANT_ID, baselineId, candidateId))
                .isInstanceOf(EvaluationComparisonException.class)
                .hasMessageContaining("相同类型");
    }

    @Test
    void hidesWhetherAnotherTenantRunExists() {
        EvaluationRepository repository = mock(EvaluationRepository.class);
        UUID runId = UUID.randomUUID();
        when(repository.findRun(TENANT_ID, runId)).thenReturn(Optional.empty());
        EvaluationHistoryService service = new EvaluationHistoryService(repository);

        assertThatThrownBy(() -> service.details(TENANT_ID, runId))
                .isInstanceOf(EvaluationRunNotFoundException.class)
                .hasMessageContaining("不存在或无权访问");
    }

    private EvaluationRunRecord run(UUID id, String type, double passRate) {
        return new EvaluationRunRecord(
                id,
                "COMPLETED",
                Map.of("type", type, "tenantId", TENANT_ID.toString()),
                new EvaluationRunSummary(10, 8, 2, 0,
                        passRate, passRate, passRate),
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:01:00Z")
        );
    }

    private EvaluationCaseResult result(UUID runId, String promptVersion) {
        return new EvaluationCaseResult(
                UUID.randomUUID(),
                runId,
                UUID.randomUUID(),
                "case",
                "question",
                "answer",
                true,
                List.of(),
                Map.of("passed", true, "promptVersion", promptVersion),
                10,
                null
        );
    }
}
