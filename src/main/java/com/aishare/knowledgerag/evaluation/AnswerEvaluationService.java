package com.aishare.knowledgerag.evaluation;

import com.aishare.knowledgerag.answer.AnswerCitation;
import com.aishare.knowledgerag.answer.GroundedAnswer;
import com.aishare.knowledgerag.answer.RagAnswerService;
import com.aishare.knowledgerag.retrieval.VectorSearchQuery;
import com.aishare.knowledgerag.security.AccessContext;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AnswerEvaluationService {

    private static final Pattern CITATION_MARKER = Pattern.compile("\\[(\\d+)]");

    private final RagAnswerService answerService;
    private final EvaluationRepository evaluationRepository;

    public AnswerEvaluationService(
            RagAnswerService answerService,
            EvaluationRepository evaluationRepository
    ) {
        this.answerService = answerService;
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
                "type", "RAG_ANSWER",
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

        EvaluationRunSummary summary = EvaluationSummaries.summarize(
                results, "expectedDocumentCited"
        );
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
            GroundedAnswer answer = answerService.answer(new VectorSearchQuery(
                    evaluationCase.question(), accessContext, null, topK, minScore
            ));
            Set<UUID> expectedDocumentIds = evaluationCase.expectedExternalDocumentIds()
                    .stream()
                    .map(expectedDocuments::get)
                    .filter(java.util.Objects::nonNull)
                    .collect(java.util.stream.Collectors.toSet());
            List<String> missingExpectedDocuments = evaluationCase
                    .expectedExternalDocumentIds().stream()
                    .filter(externalId -> !expectedDocuments.containsKey(externalId))
                    .toList();
            Set<Integer> referencedMarkers = referencedMarkers(answer.answer());
            Set<Integer> availableMarkers = answer.citations().stream()
                    .map(AnswerCitation::marker)
                    .collect(java.util.stream.Collectors.toSet());
            List<Integer> invalidCitationMarkers = referencedMarkers.stream()
                    .filter(marker -> !availableMarkers.contains(marker))
                    .sorted()
                    .toList();
            boolean expectedDocumentCited = answer.citations().stream()
                    .filter(citation -> referencedMarkers.contains(citation.marker()))
                    .map(AnswerCitation::documentId)
                    .anyMatch(expectedDocumentIds::contains);
            List<String> missingKeywords = missingKeywords(
                    answer.answer(), evaluationCase.expectedKeywords()
            );
            double keywordCoverage = evaluationCase.expectedKeywords().isEmpty()
                    ? 1.0
                    : (double) (evaluationCase.expectedKeywords().size()
                    - missingKeywords.size()) / evaluationCase.expectedKeywords().size();
            boolean hasCitation = !referencedMarkers.isEmpty();
            boolean allReferencedCitationsValid = invalidCitationMarkers.isEmpty();
            boolean decisionCorrect = evaluationCase.shouldAnswer() == answer.grounded();
            boolean passed = evaluationCase.shouldAnswer()
                    ? decisionCorrect
                    && hasCitation
                    && allReferencedCitationsValid
                    && expectedDocumentCited
                    && missingExpectedDocuments.isEmpty()
                    && missingKeywords.isEmpty()
                    : decisionCorrect;

            Map<String, Object> metrics = new LinkedHashMap<>();
            metrics.put("passed", passed);
            metrics.put("expectedShouldAnswer", evaluationCase.shouldAnswer());
            metrics.put("actualGrounded", answer.grounded());
            metrics.put("decisionCorrect", decisionCorrect);
            metrics.put("keywordCoverage", keywordCoverage);
            metrics.put("missingExpectedKeywords", missingKeywords);
            metrics.put("availableCitationCount", answer.citations().size());
            metrics.put("referencedCitationCount", referencedMarkers.size());
            metrics.put("hasCitation", hasCitation);
            metrics.put("allReferencedCitationsValid", allReferencedCitationsValid);
            metrics.put("invalidCitationMarkers", invalidCitationMarkers);
            metrics.put("expectedDocumentCited", expectedDocumentCited);
            metrics.put("missingExpectedDocuments", missingExpectedDocuments);
            metrics.put("promptVersion", answer.promptVersion());
            return result(
                    runId, evaluationCase, answer.answer(), passed,
                    toChunks(answer.citations()), metrics,
                    elapsedMillis(startedNanos), null
            );
        } catch (RuntimeException exception) {
            return result(
                    runId, evaluationCase, null, false, List.of(),
                    Map.of("passed", false, "evaluationError", true),
                    elapsedMillis(startedNanos), exception.getMessage()
            );
        }
    }

    private List<String> missingKeywords(String answer, List<String> expectedKeywords) {
        String normalizedAnswer = answer.toLowerCase(Locale.ROOT);
        return expectedKeywords.stream()
                .filter(keyword -> !normalizedAnswer.contains(
                        keyword.toLowerCase(Locale.ROOT)))
                .toList();
    }

    private Set<Integer> referencedMarkers(String answer) {
        Set<Integer> markers = new LinkedHashSet<>();
        Matcher matcher = CITATION_MARKER.matcher(answer);
        while (matcher.find()) {
            markers.add(Integer.parseInt(matcher.group(1)));
        }
        return Set.copyOf(markers);
    }

    private List<EvaluationRetrievedChunk> toChunks(List<AnswerCitation> citations) {
        return citations.stream()
                .map(citation -> new EvaluationRetrievedChunk(
                        citation.marker(), citation.chunkId(), citation.documentId(),
                        null, null, 0.0
                ))
                .toList();
    }

    private EvaluationCaseResult result(
            UUID runId,
            EvaluationCase evaluationCase,
            String answer,
            boolean passed,
            List<EvaluationRetrievedChunk> chunks,
            Map<String, Object> metrics,
            long latencyMs,
            String errorMessage
    ) {
        return new EvaluationCaseResult(
                UUID.randomUUID(), runId, evaluationCase.id(),
                evaluationCase.caseKey(), evaluationCase.question(), answer, passed,
                chunks, metrics, latencyMs, errorMessage
        );
    }

    private long elapsedMillis(long startedNanos) {
        return Duration.ofNanos(System.nanoTime() - startedNanos).toMillis();
    }
}
