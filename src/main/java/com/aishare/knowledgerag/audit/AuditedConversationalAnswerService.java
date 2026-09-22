package com.aishare.knowledgerag.audit;

import com.aishare.knowledgerag.answer.ChatGenerationException;
import com.aishare.knowledgerag.answer.ChatUnavailableException;
import com.aishare.knowledgerag.conversation.ConversationAnswer;
import com.aishare.knowledgerag.conversation.ConversationAnswerStream;
import com.aishare.knowledgerag.conversation.ConversationNotFoundException;
import com.aishare.knowledgerag.conversation.ConversationalAnswerService;
import com.aishare.knowledgerag.embedding.EmbeddingGenerationException;
import com.aishare.knowledgerag.embedding.EmbeddingUnavailableException;
import com.aishare.knowledgerag.ingestion.DocumentChecksumService;
import com.aishare.knowledgerag.retrieval.VectorSearchQuery;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import reactor.core.publisher.Flux;

@Service
public class AuditedConversationalAnswerService {

    private static final Logger log = LoggerFactory.getLogger(AuditedConversationalAnswerService.class);

    private final ConversationalAnswerService delegate;
    private final AuditPersistenceService auditPersistenceService;
    private final DocumentChecksumService checksumService;
    private final RequestIdProvider requestIdProvider;
    private final AuditProperties properties;
    private final MeterRegistry meterRegistry;

    public AuditedConversationalAnswerService(
            ConversationalAnswerService delegate,
            AuditPersistenceService auditPersistenceService,
            DocumentChecksumService checksumService,
            RequestIdProvider requestIdProvider,
            AuditProperties properties,
            MeterRegistry meterRegistry
    ) {
        this.delegate = delegate;
        this.auditPersistenceService = auditPersistenceService;
        this.checksumService = checksumService;
        this.requestIdProvider = requestIdProvider;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
    }

    public ConversationAnswer answer(
            Optional<UUID> conversationId,
            VectorSearchQuery query
    ) {
        long startedNanos = System.nanoTime();
        String requestId = requestIdProvider.currentOrCreate();
        try {
            ConversationAnswer answer = delegate.answer(conversationId, query);
            String outcome = answer.grounded() ? "SUCCESS" : "NO_EVIDENCE";
            long latencyMs = elapsedMillis(startedNanos);
            saveSafely(audit(
                    requestId,
                    answer.conversationId(),
                    query,
                    answer.citations().stream()
                            .map(citation -> citation.documentId())
                            .distinct()
                            .toList(),
                    outcome,
                    latencyMs,
                    null,
                    answer.promptVersion()
            ));
            recordMetrics(outcome, latencyMs);
            return answer;
        } catch (RuntimeException exception) {
            long latencyMs = elapsedMillis(startedNanos);
            String outcome = "FAILED";
            saveSafely(audit(
                    requestId,
                    conversationId.orElse(null),
                    query,
                    List.of(),
                    outcome,
                    latencyMs,
                    errorCode(exception),
                    "unknown"
            ));
            recordMetrics(outcome, latencyMs);
            throw exception;
        }
    }

    public ConversationAnswerStream stream(
            Optional<UUID> conversationId,
            VectorSearchQuery query
    ) {
        long startedNanos = System.nanoTime();
        String requestId = requestIdProvider.currentOrCreate();
        try {
            ConversationAnswerStream stream = delegate.stream(conversationId, query);
            AtomicBoolean recorded = new AtomicBoolean();
            Flux<String> auditedContent = stream.content()
                    .doOnComplete(() -> recordStreamResult(
                            recorded,
                            requestId,
                            stream,
                            query,
                            stream.grounded() ? "SUCCESS" : "NO_EVIDENCE",
                            startedNanos,
                            null,
                            stream.promptVersion()
                    ))
                    .doOnError(exception -> recordStreamResult(
                            recorded,
                            requestId,
                            stream,
                            query,
                            "FAILED",
                            startedNanos,
                            errorCode(exception),
                            stream.promptVersion()
                    ))
                    .doOnCancel(() -> recordStreamResult(
                            recorded,
                            requestId,
                            stream,
                            query,
                            "CANCELLED",
                            startedNanos,
                            "CLIENT_CANCELLED",
                            stream.promptVersion()
                    ));
            return stream.withContent(auditedContent);
        } catch (RuntimeException exception) {
            long latencyMs = elapsedMillis(startedNanos);
            saveSafely(audit(
                    requestId,
                    conversationId.orElse(null),
                    query,
                    List.of(),
                    "FAILED",
                    latencyMs,
                    errorCode(exception),
                    "unknown"
            ));
            recordMetrics("FAILED", latencyMs);
            throw exception;
        }
    }

    private void recordStreamResult(
            AtomicBoolean recorded,
            String requestId,
            ConversationAnswerStream stream,
            VectorSearchQuery query,
            String outcome,
            long startedNanos,
            String errorCode,
            String promptVersion
    ) {
        if (!recorded.compareAndSet(false, true)) {
            return;
        }
        long latencyMs = elapsedMillis(startedNanos);
        saveSafely(audit(
                requestId,
                stream.conversationId(),
                query,
                stream.citations().stream()
                        .map(citation -> citation.documentId())
                        .distinct()
                        .toList(),
                outcome,
                latencyMs,
                errorCode,
                promptVersion
        ));
        recordMetrics(outcome, latencyMs);
    }

    private RagRequestAudit audit(
            String requestId,
            UUID conversationId,
            VectorSearchQuery query,
            List<UUID> documentIds,
            String outcome,
            long latencyMs,
            String errorCode,
            String promptVersion
    ) {
        return new RagRequestAudit(
                UUID.randomUUID(),
                requestId,
                query.accessContext().tenantId(),
                query.accessContext().userId(),
                conversationId,
                checksumService.sha256(query.question().getBytes(StandardCharsets.UTF_8)),
                "HYBRID_RRF",
                properties.modelName(),
                promptVersion,
                documentIds,
                outcome,
                latencyMs,
                errorCode
        );
    }

    private void saveSafely(RagRequestAudit audit) {
        try {
            auditPersistenceService.save(audit);
        } catch (RuntimeException exception) {
            log.error("保存 RAG 请求审计失败 requestId={}", audit.requestId(), exception);
        }
    }

    private void recordMetrics(String outcome, long latencyMs) {
        meterRegistry.counter("rag.answer.requests", "outcome", outcome).increment();
        meterRegistry.timer("rag.answer.duration", "outcome", outcome)
                .record(Duration.ofMillis(latencyMs));
    }

    private long elapsedMillis(long startedNanos) {
        return Duration.ofNanos(System.nanoTime() - startedNanos).toMillis();
    }

    private String errorCode(Throwable exception) {
        if (exception instanceof ConversationNotFoundException) {
            return "CONVERSATION_NOT_FOUND";
        }
        if (exception instanceof EmbeddingUnavailableException) {
            return "EMBEDDING_UNAVAILABLE";
        }
        if (exception instanceof EmbeddingGenerationException) {
            return "EMBEDDING_FAILED";
        }
        if (exception instanceof ChatUnavailableException) {
            return "CHAT_UNAVAILABLE";
        }
        if (exception instanceof ChatGenerationException) {
            return "CHAT_GENERATION_FAILED";
        }
        return "INTERNAL_ERROR";
    }
}
