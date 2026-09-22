package com.aishare.knowledgerag.resilience;

import com.openai.errors.OpenAIIoException;
import com.openai.errors.OpenAIRetryableException;
import com.openai.errors.OpenAIServiceException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

@Component
public class AiResilienceExecutor {

    private final Policy embeddingPolicy;
    private final Policy chatPolicy;

    public AiResilienceExecutor(AiResilienceProperties properties) {
        this.embeddingPolicy = createPolicy("embeddingAi", properties);
        this.chatPolicy = createPolicy("chatAi", properties);
    }

    public <T> T executeEmbedding(Supplier<T> operation) {
        return embeddingPolicy.execute(operation);
    }

    public <T> T executeChat(Supplier<T> operation) {
        return chatPolicy.execute(operation);
    }

    private Policy createPolicy(String name, AiResilienceProperties properties) {
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(properties.maxAttempts())
                .waitDuration(properties.retryWait())
                .retryOnException(this::isTransientFailure)
                .build();
        CircuitBreakerConfig circuitConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(properties.failureRateThreshold())
                .slidingWindowSize(properties.slidingWindowSize())
                .minimumNumberOfCalls(properties.minimumNumberOfCalls())
                .waitDurationInOpenState(properties.openStateWait())
                .permittedNumberOfCallsInHalfOpenState(2)
                .recordException(this::isTransientFailure)
                .build();
        return new Policy(
                Retry.of(name, retryConfig),
                CircuitBreaker.of(name, circuitConfig)
        );
    }

    private boolean isTransientFailure(Throwable failure) {
        Throwable current = failure;
        for (int depth = 0; current != null && depth < 10; depth++) {
            if (current instanceof OpenAIIoException
                    || current instanceof OpenAIRetryableException
                    || current instanceof IOException
                    || current instanceof TimeoutException) {
                return true;
            }
            if (current instanceof OpenAIServiceException serviceException) {
                int status = serviceException.statusCode();
                return status == 429 || status >= 500;
            }
            current = current.getCause();
        }
        return false;
    }

    private record Policy(Retry retry, CircuitBreaker circuitBreaker) {

        private <T> T execute(Supplier<T> operation) {
            Supplier<T> retried = Retry.decorateSupplier(retry, operation);
            return CircuitBreaker.decorateSupplier(circuitBreaker, retried).get();
        }
    }
}
