package com.aishare.knowledgerag.api;

import com.aishare.knowledgerag.api.dto.VectorSearchRequest;
import com.aishare.knowledgerag.api.dto.VectorSearchResponse;
import com.aishare.knowledgerag.retrieval.RetrievalProperties;
import com.aishare.knowledgerag.retrieval.RetrievedChunk;
import com.aishare.knowledgerag.retrieval.VectorSearchService;
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
    private final RetrievalProperties retrievalProperties;

    public KnowledgeSearchController(
            VectorSearchService searchService,
            RetrievalProperties retrievalProperties
    ) {
        this.searchService = searchService;
        this.retrievalProperties = retrievalProperties;
    }

    @PostMapping("/vector")
    public VectorSearchResponse search(@Valid @RequestBody VectorSearchRequest request) {
        List<RetrievedChunk> results = searchService.search(request.toQuery(retrievalProperties));
        return new VectorSearchResponse(request.question().strip(), results.size(), results);
    }
}
