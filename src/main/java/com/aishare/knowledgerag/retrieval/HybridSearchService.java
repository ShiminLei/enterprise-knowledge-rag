package com.aishare.knowledgerag.retrieval;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class HybridSearchService {

    private final VectorSearchService vectorSearchService;
    private final KeywordSearchRepository keywordSearchRepository;
    private final RetrievalProperties properties;
    private final HybridResultReranker reranker;

    public HybridSearchService(
            VectorSearchService vectorSearchService,
            KeywordSearchRepository keywordSearchRepository,
            RetrievalProperties properties,
            HybridResultReranker reranker
    ) {
        this.vectorSearchService = vectorSearchService;
        this.keywordSearchRepository = keywordSearchRepository;
        this.properties = properties;
        this.reranker = reranker;
    }

    public List<HybridSearchResult> search(VectorSearchQuery requestQuery) {
        VectorSearchQuery vectorQuery = withTopK(requestQuery, properties.vectorTopK());
        VectorSearchQuery keywordQuery = withTopK(requestQuery, properties.keywordTopK());
        List<RetrievedChunk> vectorResults = vectorSearchService.search(vectorQuery);
        List<RetrievedChunk> keywordResults = keywordSearchRepository.search(keywordQuery);

        Map<UUID, MutableFusion> fused = new LinkedHashMap<>();
        addVectorResults(fused, vectorResults);
        addKeywordResults(fused, keywordResults);

        List<HybridSearchResult> candidates = fused.values().stream()
                .map(MutableFusion::toResult)
                .toList();
        return reranker.rerank(candidates).stream()
                .limit(requestQuery.topK())
                .toList();
    }

    private VectorSearchQuery withTopK(VectorSearchQuery query, int topK) {
        return new VectorSearchQuery(
                query.question(),
                query.accessContext(),
                query.category(),
                topK,
                query.minScore()
        );
    }

    private void addVectorResults(
            Map<UUID, MutableFusion> fused,
            List<RetrievedChunk> results
    ) {
        for (int index = 0; index < results.size(); index++) {
            RetrievedChunk chunk = results.get(index);
            MutableFusion entry = fused.computeIfAbsent(
                    chunk.chunkId(),
                    ignored -> new MutableFusion(chunk)
            );
            entry.vectorRank = index + 1;
            entry.vectorScore = chunk.score();
        }
    }

    private void addKeywordResults(
            Map<UUID, MutableFusion> fused,
            List<RetrievedChunk> results
    ) {
        for (int index = 0; index < results.size(); index++) {
            RetrievedChunk chunk = results.get(index);
            MutableFusion entry = fused.computeIfAbsent(
                    chunk.chunkId(),
                    ignored -> new MutableFusion(chunk)
            );
            entry.keywordRank = index + 1;
            entry.keywordScore = chunk.score();
        }
    }

    private class MutableFusion {

        private final RetrievedChunk chunk;
        private Integer vectorRank;
        private Integer keywordRank;
        private Double vectorScore;
        private Double keywordScore;

        private MutableFusion(RetrievedChunk chunk) {
            this.chunk = chunk;
        }

        private HybridSearchResult toResult() {
            double score = 0;
            if (vectorRank != null) {
                score += 1.0 / (properties.rrfK() + vectorRank);
            }
            if (keywordRank != null) {
                score += 1.0 / (properties.rrfK() + keywordRank);
            }
            return new HybridSearchResult(
                    chunk,
                    vectorRank,
                    keywordRank,
                    vectorScore,
                    keywordScore,
                    score
            );
        }
    }
}
