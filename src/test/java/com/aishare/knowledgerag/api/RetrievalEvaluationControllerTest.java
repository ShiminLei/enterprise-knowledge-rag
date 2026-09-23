package com.aishare.knowledgerag.api;

import com.aishare.knowledgerag.common.ApiExceptionHandler;
import com.aishare.knowledgerag.evaluation.EvaluationRunReport;
import com.aishare.knowledgerag.evaluation.EvaluationRunSummary;
import com.aishare.knowledgerag.evaluation.AnswerEvaluationService;
import com.aishare.knowledgerag.evaluation.EvaluationHistoryService;
import com.aishare.knowledgerag.evaluation.EvaluationMarkdownReportService;
import com.aishare.knowledgerag.evaluation.EvaluationRunRecord;
import com.aishare.knowledgerag.evaluation.RetrievalEvaluationService;
import com.aishare.knowledgerag.retrieval.RetrievalProperties;
import com.aishare.knowledgerag.security.AccessContext;
import com.aishare.knowledgerag.security.AccessContextService;
import com.aishare.knowledgerag.security.AuthenticatedIdentity;
import com.aishare.knowledgerag.security.CurrentAuthenticatedIdentityProvider;
import com.aishare.knowledgerag.security.PermissionLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Map;
import java.time.Instant;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RetrievalEvaluationControllerTest {

    private MockMvc mockMvc;
    private RetrievalEvaluationService evaluationService;
    private AnswerEvaluationService answerEvaluationService;
    private EvaluationHistoryService historyService;
    private EvaluationMarkdownReportService markdownReportService;
    private AccessContext accessContext;

    @BeforeEach
    void setUp() {
        evaluationService = mock(RetrievalEvaluationService.class);
        answerEvaluationService = mock(AnswerEvaluationService.class);
        historyService = mock(EvaluationHistoryService.class);
        markdownReportService = mock(EvaluationMarkdownReportService.class);
        CurrentAuthenticatedIdentityProvider identityProvider =
                mock(CurrentAuthenticatedIdentityProvider.class);
        AccessContextService accessContextService = mock(AccessContextService.class);
        UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        when(identityProvider.current())
                .thenReturn(new AuthenticatedIdentity(tenantId, "zhangsan"));
        accessContext = new AccessContext(
                tenantId, "zhangsan", Set.of("信息技术部"), PermissionLevel.INTERNAL
        );
        when(accessContextService.resolve(tenantId, "zhangsan"))
                .thenReturn(accessContext);
        UUID runId = UUID.fromString("40000000-0000-0000-0000-000000000001");
        when(evaluationService.run(accessContext, 5, 0.35))
                .thenReturn(new EvaluationRunReport(
                        runId,
                        "COMPLETED",
                        new EvaluationRunSummary(5, 4, 1, 0, 0.8, 0.8, 1.0),
                        List.of()
                ));
        when(answerEvaluationService.run(accessContext, 5, 0.35))
                .thenReturn(new EvaluationRunReport(
                        runId,
                        "COMPLETED",
                        new EvaluationRunSummary(5, 4, 1, 0, 0.8, 0.8, 1.0),
                        List.of()
                ));
        mockMvc = MockMvcBuilders.standaloneSetup(new RetrievalEvaluationController(
                        evaluationService,
                        answerEvaluationService,
                        historyService,
                        markdownReportService,
                        new RetrievalProperties(20, 20, 5, 0.35, 60),
                        identityProvider,
                        accessContextService
                ))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void downloadsMarkdownReportForCurrentTenant() throws Exception {
        UUID runId = UUID.fromString("40000000-0000-0000-0000-000000000003");
        when(markdownReportService.render(accessContext.tenantId(), runId))
                .thenReturn("# RAG 评测报告\n\n通过率：100%\n");

        mockMvc.perform(get("/api/v1/admin/evaluations/runs/{runId}/report.md", runId))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .content().contentTypeCompatibleWith("text/markdown"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .content().string(org.hamcrest.Matchers.containsString("RAG 评测报告")));

        verify(markdownReportService).render(accessContext.tenantId(), runId);
    }

    @Test
    void runsEvaluationWithDefaultRetrievalSettings() throws Exception {
        mockMvc.perform(post("/api/v1/admin/evaluations/retrieval-runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.summary.passRate").value(0.8));

        verify(evaluationService).run(accessContext, 5, 0.35);
    }

    @Test
    void rejectsInvalidEvaluationSettings() throws Exception {
        mockMvc.perform(post("/api/v1/admin/evaluations/retrieval-runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"topK\":0,\"minScore\":1.5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void runsAnswerQualityEvaluation() throws Exception {
        mockMvc.perform(post("/api/v1/admin/evaluations/answer-runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        verify(answerEvaluationService).run(accessContext, 5, 0.35);
    }

    @Test
    void listsOnlyCurrentTenantEvaluationRuns() throws Exception {
        UUID tenantId = accessContext.tenantId();
        UUID runId = UUID.fromString("40000000-0000-0000-0000-000000000002");
        when(historyService.list(tenantId, 10)).thenReturn(List.of(
                new EvaluationRunRecord(
                        runId,
                        "COMPLETED",
                        Map.of("type", "RAG_ANSWER"),
                        new EvaluationRunSummary(5, 5, 0, 0, 1, 1, 1),
                        Instant.parse("2026-01-01T00:00:00Z"),
                        Instant.parse("2026-01-01T00:01:00Z")
                )
        ));

        mockMvc.perform(get("/api/v1/admin/evaluations/runs").param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].runId").value(runId.toString()))
                .andExpect(jsonPath("$[0].summary.passRate").value(1.0));

        verify(historyService).list(tenantId, 10);
    }
}
