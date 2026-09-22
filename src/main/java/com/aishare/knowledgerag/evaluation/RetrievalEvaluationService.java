package com.aishare.knowledgerag.evaluation;

import com.aishare.knowledgerag.retrieval.HybridSearchResult;
import com.aishare.knowledgerag.retrieval.HybridSearchService;
import com.aishare.knowledgerag.retrieval.VectorSearchQuery;
import com.aishare.knowledgerag.security.AccessContext;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class RetrievalEvaluationService {

    private final HybridSearchService hybridSearchService;
    private final EvaluationRepository evaluationRepository;

    public RetrievalEvaluationService(
            HybridSearchService hybridSearchService,
            EvaluationRepository evaluationRepository
    ) {
        this.hybridSearchService = hybridSearchService;
        this.evaluationRepository = evaluationRepository;
    }

    public EvaluationRunReport run(
            AccessContext accessContext,
            int topK,
            double minScore
    ) {
        UUID runId = UUID.randomUUID();
        List<EvaluationCase> cases = evaluationRepository.findCases();
        Map<String, UUID> expectedDocuments = evaluationRepository.resolveDocumentIds(
                accessContext.tenantId(),
                cases.stream()
                        .flatMap(item -> item.expectedExternalDocumentIds().stream())
                        .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new))
        );
        evaluationRepository.startRun(runId, Map.of(
                "type", "HYBRID_RETRIEVAL",
                "topK", topK,
                "minScore", minScore,
                "tenantId", accessContext.tenantId().toString(),
                "evaluationUserId", accessContext.userId()
        ), Instant.now());

        List<EvaluationCaseResult> results = new ArrayList<>();
        for (EvaluationCase evaluationCase : cases) {
            EvaluationCaseResult result = evaluateCase(
                    runId, evaluationCase, expectedDocuments,
                    accessContext, topK, minScore
            );
            evaluationRepository.saveResult(result);
            results.add(result);
        }

        EvaluationRunSummary summary = EvaluationSummaries.summarize(results, "hitAtK");
        String status = summary.errorCases() == 0
                ? "COMPLETED"
                : "COMPLETED_WITH_ERRORS";
        evaluationRepository.completeRun(runId, status, summary, Instant.now());
        return new EvaluationRunReport(runId, status, summary, results);
    }

    private EvaluationCaseResult evaluateCase(
            UUID runId,
            EvaluationCase evaluationCase,
            Map<String, UUID> expectedDocuments,
            AccessContext accessContext,
            int topK,
            double minScore
    ) {
        long startedNanos = System.nanoTime();
        try {
            List<HybridSearchResult> retrieved = hybridSearchService.search(
                    new VectorSearchQuery(
                            evaluationCase.question(), accessContext, null, topK, minScore
                    )
            );
            List<EvaluationRetrievedChunk> chunks = toChunks(retrieved);
            Set<UUID> retrievedDocumentIds = retrieved.stream()
                    .map(result -> result.chunk().documentId())
                    .collect(java.util.stream.Collectors.toSet());
            Set<UUID> expectedDocumentIds = evaluationCase.expectedExternalDocumentIds()
                    .stream()
                    .map(expectedDocuments::get)
                    .filter(java.util.Objects::nonNull)
                    .collect(java.util.stream.Collectors.toSet());
            List<String> missingExpectedDocuments = evaluationCase
                    .expectedExternalDocumentIds().stream()
                    .filter(externalId -> !expectedDocuments.containsKey(externalId))
                    .toList();
            long matchedDocumentCount = expectedDocumentIds.stream()
                    .filter(retrievedDocumentIds::contains)
                    .count();
            boolean actualHasEvidence = !retrieved.isEmpty();
            boolean decisionCorrect = evaluationCase.shouldAnswer() == actualHasEvidence;
            boolean hitAtK = matchedDocumentCount > 0;
            double documentRecall = expectedDocumentIds.isEmpty()
                    ? 0.0
                    : (double) matchedDocumentCount / expectedDocumentIds.size();
            boolean passed = decisionCorrect
                    && (!evaluationCase.shouldAnswer()
                    || (hitAtK && missingExpectedDocuments.isEmpty()));

            Map<String, Object> metrics = new LinkedHashMap<>();
            metrics.put("passed", passed);
            metrics.put("expectedShouldAnswer", evaluationCase.shouldAnswer());
            metrics.put("actualHasEvidence", actualHasEvidence);
            metrics.put("decisionCorrect", decisionCorrect);
            metrics.put("hitAtK", hitAtK);
            metrics.put("documentRecall", documentRecall);
            metrics.put("expectedDocumentCount",
                    evaluationCase.expectedExternalDocumentIds().size());
            metrics.put("resolvedExpectedDocumentCount", expectedDocumentIds.size());
            metrics.put("retrievedDocumentCount", retrievedDocumentIds.size());
            metrics.put("matchedDocumentCount", matchedDocumentCount);
            metrics.put("missingExpectedDocuments", missingExpectedDocuments);
            return result(
                    runId, evaluationCase, passed, chunks, metrics,
                    elapsedMillis(startedNanos), null
            );
        } catch (RuntimeException exception) {
            return result(
                    runId, evaluationCase, false, List.of(),
                    Map.of("passed", false, "evaluationError", true),
                    elapsedMillis(startedNanos), exception.getMessage()
            );
        }
    }

    private List<EvaluationRetrievedChunk> toChunks(List<HybridSearchResult> retrieved) {
        List<EvaluationRetrievedChunk> chunks = new ArrayList<>();
        for (int index = 0; index < retrieved.size(); index++) {
            HybridSearchResult result = retrieved.get(index);
            chunks.add(new EvaluationRetrievedChunk(
                    index + 1,
                    result.chunk().chunkId(),
                    result.chunk().documentId(),
                    result.vectorRank(),
                    result.keywordRank(),
                    result.rrfScore()
            ));
        }
        return List.copyOf(chunks);
    }

    private EvaluationCaseResult result(
            UUID runId,
            EvaluationCase evaluationCase,
            boolean passed,
            List<EvaluationRetrievedChunk> chunks,
            Map<String, Object> metrics,
            long latencyMs,
            String errorMessage
    ) {
        return new EvaluationCaseResult(
                UUID.randomUUID(), runId, evaluationCase.id(),
                evaluationCase.caseKey(), evaluationCase.question(), null, passed,
                chunks, metrics, latencyMs, errorMessage
        );
    }

    private long elapsedMillis(long startedNanos) {
        return Duration.ofNanos(System.nanoTime() - startedNanos).toMillis();
    }
}
