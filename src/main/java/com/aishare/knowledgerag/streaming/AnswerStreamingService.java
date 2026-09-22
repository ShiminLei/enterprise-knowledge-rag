package com.aishare.knowledgerag.streaming;

import com.aishare.knowledgerag.audit.AuditedConversationalAnswerService;
import com.aishare.knowledgerag.audit.RequestIdProvider;
import com.aishare.knowledgerag.conversation.ConversationAnswer;
import com.aishare.knowledgerag.retrieval.VectorSearchQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class AnswerStreamingService {

    private static final Logger log = LoggerFactory.getLogger(AnswerStreamingService.class);

    private final AuditedConversationalAnswerService answerService;
    private final RequestIdProvider requestIdProvider;
    private final AnswerStreamingProperties properties;
    private final Executor executor;

    public AnswerStreamingService(
            AuditedConversationalAnswerService answerService,
            RequestIdProvider requestIdProvider,
            AnswerStreamingProperties properties,
            @Qualifier("answerStreamExecutor") Executor executor
    ) {
        this.answerService = answerService;
        this.requestIdProvider = requestIdProvider;
        this.properties = properties;
        this.executor = executor;
    }

    public SseEmitter stream(
            Optional<UUID> conversationId,
            VectorSearchQuery query
    ) {
        SseEmitter emitter = new SseEmitter(properties.timeout().toMillis());
        AtomicBoolean closed = new AtomicBoolean();
        emitter.onCompletion(() -> closed.set(true));
        emitter.onTimeout(() -> closed.set(true));
        emitter.onError(ignored -> closed.set(true));

        String requestId = requestIdProvider.currentOrCreate();
        Map<String, String> loggingContext = MDC.getCopyOfContextMap();
        try {
            executor.execute(() -> withLoggingContext(loggingContext,
                    () -> produceEvents(emitter, closed, requestId, conversationId, query)));
        } catch (RejectedExecutionException exception) {
            sendSafely(emitter, closed, "error", new AnswerStreamError(
                    "STREAM_BUSY", "当前流式请求过多，请稍后重试"
            ));
            emitter.complete();
        }
        return emitter;
    }

    private void produceEvents(
            SseEmitter emitter,
            AtomicBoolean closed,
            String requestId,
            Optional<UUID> conversationId,
            VectorSearchQuery query
    ) {
        try {
            if (!sendSafely(emitter, closed, "started", new AnswerStreamStarted(requestId))) {
                return;
            }
            ConversationAnswer answer = answerService.answer(conversationId, query);
            int sequence = 0;
            for (String content : splitByCodePoints(
                    answer.answer(), properties.chunkCharacters())) {
                if (!sendSafely(emitter, closed, "delta",
                        new AnswerStreamDelta(++sequence, content))) {
                    return;
                }
            }
            if (!sendSafely(emitter, closed, "citations",
                    new AnswerStreamCitations(answer.citations()))) {
                return;
            }
            sendSafely(emitter, closed, "completed", new AnswerStreamCompleted(
                    answer.conversationId(), answer.grounded(), answer.retrievedCount()
            ));
            emitter.complete();
        } catch (RuntimeException exception) {
            log.warn("流式回答失败 requestId={}", requestId, exception);
            sendSafely(emitter, closed, "error", new AnswerStreamError(
                    "ANSWER_STREAM_FAILED", "生成回答失败，请稍后重试"
            ));
            emitter.complete();
        }
    }

    private boolean sendSafely(
            SseEmitter emitter,
            AtomicBoolean closed,
            String eventName,
            Object data
    ) {
        if (closed.get()) {
            return false;
        }
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
            return true;
        } catch (IOException | IllegalStateException exception) {
            closed.set(true);
            log.debug("客户端已断开 SSE 连接 event={}", eventName, exception);
            return false;
        }
    }

    private List<String> splitByCodePoints(String text, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        int offset = 0;
        while (offset < text.length()) {
            int remainingCodePoints = text.codePointCount(offset, text.length());
            int codePoints = Math.min(chunkSize, remainingCodePoints);
            int end = text.offsetByCodePoints(offset, codePoints);
            chunks.add(text.substring(offset, end));
            offset = end;
        }
        return chunks;
    }

    private void withLoggingContext(Map<String, String> context, Runnable task) {
        Map<String, String> previous = MDC.getCopyOfContextMap();
        try {
            if (context == null) {
                MDC.clear();
            } else {
                MDC.setContextMap(context);
            }
            task.run();
        } finally {
            if (previous == null) {
                MDC.clear();
            } else {
                MDC.setContextMap(previous);
            }
        }
    }
}
