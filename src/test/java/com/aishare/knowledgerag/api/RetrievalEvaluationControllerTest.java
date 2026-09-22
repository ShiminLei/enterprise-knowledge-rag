package com.aishare.knowledgerag.api;

import com.aishare.knowledgerag.common.ApiExceptionHandler;
import com.aishare.knowledgerag.evaluation.EvaluationRunReport;
import com.aishare.knowledgerag.evaluation.EvaluationRunSummary;
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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RetrievalEvaluationControllerTest {

    private MockMvc mockMvc;
    private RetrievalEvaluationService evaluationService;
    private AccessContext accessContext;

    @BeforeEach
    void setUp() {
        evaluationService = mock(RetrievalEvaluationService.class);
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
        mockMvc = MockMvcBuilders.standaloneSetup(new RetrievalEvaluationController(
                        evaluationService,
                        new RetrievalProperties(20, 20, 5, 0.35, 60),
                        identityProvider,
                        accessContextService
                ))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
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
}
