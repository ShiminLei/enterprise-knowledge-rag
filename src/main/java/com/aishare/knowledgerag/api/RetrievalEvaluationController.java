package com.aishare.knowledgerag.api;

import com.aishare.knowledgerag.api.dto.RetrievalEvaluationRequest;
import com.aishare.knowledgerag.evaluation.EvaluationRunReport;
import com.aishare.knowledgerag.evaluation.RetrievalEvaluationService;
import com.aishare.knowledgerag.retrieval.RetrievalProperties;
import com.aishare.knowledgerag.security.AccessContext;
import com.aishare.knowledgerag.security.AccessContextService;
import com.aishare.knowledgerag.security.AuthenticatedIdentity;
import com.aishare.knowledgerag.security.CurrentAuthenticatedIdentityProvider;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/evaluations")
public class RetrievalEvaluationController {

    private final RetrievalEvaluationService evaluationService;
    private final RetrievalProperties retrievalProperties;
    private final CurrentAuthenticatedIdentityProvider identityProvider;
    private final AccessContextService accessContextService;

    public RetrievalEvaluationController(
            RetrievalEvaluationService evaluationService,
            RetrievalProperties retrievalProperties,
            CurrentAuthenticatedIdentityProvider identityProvider,
            AccessContextService accessContextService
    ) {
        this.evaluationService = evaluationService;
        this.retrievalProperties = retrievalProperties;
        this.identityProvider = identityProvider;
        this.accessContextService = accessContextService;
    }

    @PostMapping("/retrieval-runs")
    public EvaluationRunReport run(
            @Valid @RequestBody RetrievalEvaluationRequest request
    ) {
        AuthenticatedIdentity identity = identityProvider.current();
        AccessContext accessContext = accessContextService.resolve(
                identity.tenantId(), identity.userId()
        );
        return evaluationService.run(
                accessContext,
                request.resolvedTopK(retrievalProperties.finalTopK()),
                request.resolvedMinScore(retrievalProperties.minScore())
        );
    }
}
