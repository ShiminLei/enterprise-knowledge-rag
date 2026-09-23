package com.aishare.knowledgerag.api;

import com.aishare.knowledgerag.api.dto.RetrievalEvaluationRequest;
import com.aishare.knowledgerag.evaluation.EvaluationRunReport;
import com.aishare.knowledgerag.evaluation.AnswerEvaluationService;
import com.aishare.knowledgerag.evaluation.EvaluationHistoryService;
import com.aishare.knowledgerag.evaluation.EvaluationRunComparison;
import com.aishare.knowledgerag.evaluation.EvaluationRunDetails;
import com.aishare.knowledgerag.evaluation.EvaluationRunRecord;
import com.aishare.knowledgerag.evaluation.EvaluationMarkdownReportService;
import com.aishare.knowledgerag.evaluation.RetrievalEvaluationService;
import com.aishare.knowledgerag.retrieval.RetrievalProperties;
import com.aishare.knowledgerag.security.AccessContext;
import com.aishare.knowledgerag.security.AccessContextService;
import com.aishare.knowledgerag.security.AuthenticatedIdentity;
import com.aishare.knowledgerag.security.CurrentAuthenticatedIdentityProvider;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/admin/evaluations")
public class RetrievalEvaluationController {

    private final RetrievalEvaluationService evaluationService;
    private final AnswerEvaluationService answerEvaluationService;
    private final EvaluationHistoryService historyService;
    private final EvaluationMarkdownReportService markdownReportService;
    private final RetrievalProperties retrievalProperties;
    private final CurrentAuthenticatedIdentityProvider identityProvider;
    private final AccessContextService accessContextService;

    public RetrievalEvaluationController(
            RetrievalEvaluationService evaluationService,
            AnswerEvaluationService answerEvaluationService,
            EvaluationHistoryService historyService,
            EvaluationMarkdownReportService markdownReportService,
            RetrievalProperties retrievalProperties,
            CurrentAuthenticatedIdentityProvider identityProvider,
            AccessContextService accessContextService
    ) {
        this.evaluationService = evaluationService;
        this.answerEvaluationService = answerEvaluationService;
        this.historyService = historyService;
        this.markdownReportService = markdownReportService;
        this.retrievalProperties = retrievalProperties;
        this.identityProvider = identityProvider;
        this.accessContextService = accessContextService;
    }

    @PostMapping("/retrieval-runs")
    public EvaluationRunReport run(
            @Valid @RequestBody RetrievalEvaluationRequest request
    ) {
        return evaluationService.run(
                currentAccessContext(),
                request.resolvedTopK(retrievalProperties.finalTopK()),
                request.resolvedMinScore(retrievalProperties.minScore())
        );
    }

    @PostMapping("/answer-runs")
    public EvaluationRunReport runAnswers(
            @Valid @RequestBody RetrievalEvaluationRequest request
    ) {
        return answerEvaluationService.run(
                currentAccessContext(),
                request.resolvedTopK(retrievalProperties.finalTopK()),
                request.resolvedMinScore(retrievalProperties.minScore())
        );
    }

    @GetMapping("/runs")
    public List<EvaluationRunRecord> listRuns(
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit
    ) {
        return historyService.list(identityProvider.current().tenantId(), limit);
    }

    @GetMapping("/runs/{runId}")
    public EvaluationRunDetails runDetails(@PathVariable UUID runId) {
        return historyService.details(identityProvider.current().tenantId(), runId);
    }

    @GetMapping(value = "/runs/{runId}/report.md", produces = "text/markdown;charset=UTF-8")
    public ResponseEntity<String> markdownReport(@PathVariable UUID runId) {
        String body = markdownReportService.render(
                identityProvider.current().tenantId(), runId
        );
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=rag-evaluation-" + runId + ".md")
                .contentType(MediaType.parseMediaType("text/markdown;charset=UTF-8"))
                .body(body);
    }

    @GetMapping("/comparisons")
    public EvaluationRunComparison compare(
            @RequestParam UUID baselineRunId,
            @RequestParam UUID candidateRunId
    ) {
        return historyService.compare(
                identityProvider.current().tenantId(),
                baselineRunId,
                candidateRunId
        );
    }

    private AccessContext currentAccessContext() {
        AuthenticatedIdentity identity = identityProvider.current();
        return accessContextService.resolve(identity.tenantId(), identity.userId());
    }
}
