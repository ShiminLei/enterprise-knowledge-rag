package com.aishare.knowledgerag.streaming;

import com.aishare.knowledgerag.audit.AuditedConversationalAnswerService;
import com.aishare.knowledgerag.audit.RequestIdProvider;
import com.aishare.knowledgerag.conversation.ConversationAnswerStream;
import com.aishare.knowledgerag.retrieval.VectorSearchQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.reactivestreams.Subscription;
import reactor.core.Disposable;
import reactor.core.publisher.BaseSubscriber;

@Service
public class AnswerStreamingService {

    private static final Logger log = LoggerFactory.getLogger(AnswerStreamingService.class);

    private final AuditedConversationalAnswerService answerService;
    private final RequestIdProvider requestIdProvider;
    private final AnswerStreamingProperties properties;
    private final Executor executor;
    private final Semaphore activeStreams;

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
        this.activeStreams = new Semaphore(properties.maxActiveStreams());
    }

    public SseEmitter stream(
            Optional<UUID> conversationId,
            VectorSearchQuery query
    ) {
        SseEmitter emitter = new SseEmitter(properties.timeout().toMillis());
        if (!activeStreams.tryAcquire()) {
            sendSafely(emitter, new AtomicBoolean(), "error", new AnswerStreamError(
                    "STREAM_BUSY", "当前流式连接过多，请稍后重试"
            ));
            emitter.complete();
            return emitter;
        }
        AtomicBoolean closed = new AtomicBoolean();
        AtomicBoolean permitReleased = new AtomicBoolean();
        AtomicReference<Disposable> subscription = new AtomicReference<>();
        Runnable releasePermit = () -> {
            if (permitReleased.compareAndSet(false, true)) {
                activeStreams.release();
            }
        };
        Runnable close = () -> {
            closed.set(true);
            Disposable disposable = subscription.get();
            if (disposable != null && !disposable.isDisposed()) {
                disposable.dispose();
            }
            releasePermit.run();
        };
        emitter.onCompletion(close);
        emitter.onTimeout(close);
        emitter.onError(ignored -> close.run());

        String requestId = requestIdProvider.currentOrCreate();
        Map<String, String> loggingContext = MDC.getCopyOfContextMap();
        try {
            executor.execute(() -> withLoggingContext(loggingContext,
                    () -> produceEvents(
                            emitter,
                            closed,
                            subscription,
                            releasePermit,
                            loggingContext,
                            requestId,
                            conversationId,
                            query
                    )));
        } catch (RejectedExecutionException exception) {
            sendSafely(emitter, closed, "error", new AnswerStreamError(
                    "STREAM_BUSY", "当前流式请求过多，请稍后重试"
            ));
            releasePermit.run();
            emitter.complete();
        }
        return emitter;
    }

    private void produceEvents(
            SseEmitter emitter,
            AtomicBoolean closed,
            AtomicReference<Disposable> subscription,
            Runnable releasePermit,
            Map<String, String> loggingContext,
            String requestId,
            Optional<UUID> conversationId,
            VectorSearchQuery query
    ) {
        try {
            if (!sendSafely(emitter, closed, "started", new AnswerStreamStarted(requestId))) {
                return;
            }
            ConversationAnswerStream answer = answerService.stream(conversationId, query);
            AtomicInteger sequence = new AtomicInteger();
            BaseSubscriber<String> subscriber = new BaseSubscriber<>() {
                @Override
                protected void hookOnSubscribe(Subscription value) {
                    subscription.set(this);
                    if (closed.get()) {
                        cancel();
                    } else {
                        request(1);
                    }
                }

                @Override
                protected void hookOnNext(String content) {
                    withLoggingContext(loggingContext, () -> {
                        if (sendSafely(emitter, closed, "delta", new AnswerStreamDelta(
                                sequence.incrementAndGet(), content
                        ))) {
                            request(1);
                        } else {
                            cancel();
                        }
                    });
                }

                @Override
                protected void hookOnComplete() {
                    withLoggingContext(loggingContext, () -> {
                        if (!sendSafely(emitter, closed, "citations",
                                new AnswerStreamCitations(answer.citations()))) {
                            releasePermit.run();
                            emitter.complete();
                            return;
                        }
                        sendSafely(emitter, closed, "completed", new AnswerStreamCompleted(
                                answer.conversationId(),
                                answer.grounded(),
                                answer.retrievedCount()
                        ));
                        releasePermit.run();
                        emitter.complete();
                    });
                }

                @Override
                protected void hookOnError(Throwable exception) {
                    withLoggingContext(loggingContext, () -> {
                        releasePermit.run();
                        completeWithSafeError(emitter, closed, requestId, exception);
                    });
                }

                @Override
                protected void hookOnCancel() {
                    releasePermit.run();
                }
            };
            answer.content().subscribe(subscriber);
        } catch (RuntimeException exception) {
            releasePermit.run();
            completeWithSafeError(emitter, closed, requestId, exception);
        }
    }

    private void completeWithSafeError(
            SseEmitter emitter,
            AtomicBoolean closed,
            String requestId,
            Throwable exception
    ) {
        log.warn("流式回答失败 requestId={}", requestId, exception);
        sendSafely(emitter, closed, "error", new AnswerStreamError(
                "ANSWER_STREAM_FAILED", "生成回答失败，请稍后重试"
        ));
        emitter.complete();
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
