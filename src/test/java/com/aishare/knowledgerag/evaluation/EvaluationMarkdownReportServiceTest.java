package com.aishare.knowledgerag.evaluation;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EvaluationMarkdownReportServiceTest {

    @Test
    void rendersPortableMarkdownAndEscapesTableCells() {
        UUID tenantId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();
        EvaluationHistoryService history = mock(EvaluationHistoryService.class);
        EvaluationRunRecord run = new EvaluationRunRecord(
                runId, "COMPLETED", Map.of("type", "RAG_ANSWER"),
                new EvaluationRunSummary(1, 1, 0, 0, 1, 1, 1),
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:01Z")
        );
        EvaluationCaseResult result = new EvaluationCaseResult(
                UUID.randomUUID(), runId, UUID.randomUUID(), "vpn|login",
                "如何登录 VPN？", "使用公司账号。[1]", true, List.of(),
                Map.of("citationAccuracy", 1.0), 12, null
        );
        when(history.details(tenantId, runId)).thenReturn(
                new EvaluationRunDetails(run, Set.of("v1"), List.of(result))
        );

        String markdown = new EvaluationMarkdownReportService(history)
                .render(tenantId, runId);

        assertThat(markdown)
                .contains("# RAG 评测报告", "100.00%", "vpn\\|login")
                .contains("citationAccuracy=1.0");
    }
}
