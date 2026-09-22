package com.aishare.knowledgerag.api;

import com.aishare.knowledgerag.api.dto.VectorSearchRequest;
import com.aishare.knowledgerag.api.dto.VectorSearchResponse;
import com.aishare.knowledgerag.api.dto.HybridSearchRequest;
import com.aishare.knowledgerag.api.dto.HybridSearchResponse;
import com.aishare.knowledgerag.retrieval.HybridSearchResult;
import com.aishare.knowledgerag.retrieval.HybridSearchService;
import com.aishare.knowledgerag.retrieval.RetrievalProperties;
import com.aishare.knowledgerag.retrieval.RetrievedChunk;
import com.aishare.knowledgerag.retrieval.VectorSearchService;
import com.aishare.knowledgerag.security.AccessContext;
import com.aishare.knowledgerag.security.AccessContextService;
import com.aishare.knowledgerag.security.AuthenticatedIdentity;
import com.aishare.knowledgerag.security.CurrentAuthenticatedIdentityProvider;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
public class KnowledgeSearchController {

    private final VectorSearchService searchService;
    private final HybridSearchService hybridSearchService;
    private final RetrievalProperties retrievalProperties;
    private final AccessContextService accessContextService;
    private final CurrentAuthenticatedIdentityProvider identityProvider;

    public KnowledgeSearchController(
            VectorSearchService searchService,
            HybridSearchService hybridSearchService,
            RetrievalProperties retrievalProperties,
            AccessContextService accessContextService,
            CurrentAuthenticatedIdentityProvider identityProvider
    ) {
        this.searchService = searchService;
        this.hybridSearchService = hybridSearchService;
        this.retrievalProperties = retrievalProperties;
        this.accessContextService = accessContextService;
        this.identityProvider = identityProvider;
    }

    @PostMapping("/vector")
    public VectorSearchResponse search(@Valid @RequestBody VectorSearchRequest request) {
        AuthenticatedIdentity identity = identityProvider.current();
        AccessContext accessContext = accessContextService.resolve(
                identity.tenantId(), identity.userId());
        List<RetrievedChunk> results = searchService.search(
                request.toQuery(retrievalProperties, accessContext)
        );
        return new VectorSearchResponse(request.question().strip(), results.size(), results);
    }

    @PostMapping("/hybrid")
    public HybridSearchResponse hybridSearch(@Valid @RequestBody HybridSearchRequest request) {
        AuthenticatedIdentity identity = identityProvider.current();
        AccessContext accessContext = accessContextService.resolve(
                identity.tenantId(), identity.userId());
        List<HybridSearchResult> results = hybridSearchService.search(
                request.toQuery(retrievalProperties, accessContext)
        );
        return new HybridSearchResponse(request.question().strip(), results.size(), results);
    }
}
