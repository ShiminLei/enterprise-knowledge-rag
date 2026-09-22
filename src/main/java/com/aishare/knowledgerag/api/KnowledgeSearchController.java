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

    public KnowledgeSearchController(
            VectorSearchService searchService,
            HybridSearchService hybridSearchService,
            RetrievalProperties retrievalProperties,
            AccessContextService accessContextService
    ) {
        this.searchService = searchService;
        this.hybridSearchService = hybridSearchService;
        this.retrievalProperties = retrievalProperties;
        this.accessContextService = accessContextService;
    }

    @PostMapping("/vector")
    public VectorSearchResponse search(@Valid @RequestBody VectorSearchRequest request) {
        AccessContext accessContext = accessContextService.resolve(
                request.tenantId(), request.userId()
        );
        List<RetrievedChunk> results = searchService.search(
                request.toQuery(retrievalProperties, accessContext)
        );
        return new VectorSearchResponse(request.question().strip(), results.size(), results);
    }

    @PostMapping("/hybrid")
    public HybridSearchResponse hybridSearch(@Valid @RequestBody HybridSearchRequest request) {
        AccessContext accessContext = accessContextService.resolve(
                request.tenantId(), request.userId()
        );
        List<HybridSearchResult> results = hybridSearchService.search(
                request.toQuery(retrievalProperties, accessContext)
        );
        return new HybridSearchResponse(request.question().strip(), results.size(), results);
    }
}
