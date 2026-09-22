package com.aishare.knowledgerag.resilience;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiResilienceExecutorTest {

    @Test
    void retriesTransientIoFailureUpToConfiguredAttempts() {
        AiResilienceExecutor executor = executor(3, 5);
        AtomicInteger attempts = new AtomicInteger();

        assertThatThrownBy(() -> executor.executeEmbedding(() -> {
            attempts.incrementAndGet();
            throw new RuntimeException(new IOException("temporary network failure"));
        })).isInstanceOf(RuntimeException.class);

        assertThat(attempts).hasValue(3);
    }

    @Test
    void doesNotRetryPermanentFailure() {
        AiResilienceExecutor executor = executor(3, 5);
        AtomicInteger attempts = new AtomicInteger();

        assertThatThrownBy(() -> executor.executeChat(() -> {
            attempts.incrementAndGet();
            throw new IllegalArgumentException("bad request");
        })).isInstanceOf(IllegalArgumentException.class);

        assertThat(attempts).hasValue(1);
    }

    @Test
    void opensCircuitAfterRepeatedTransientFailures() {
        AiResilienceExecutor executor = executor(1, 2);
        AtomicInteger attempts = new AtomicInteger();
        for (int call = 0; call < 2; call++) {
            assertThatThrownBy(() -> executor.executeChat(() -> {
                attempts.incrementAndGet();
                throw new RuntimeException(new IOException("down"));
            })).isInstanceOf(RuntimeException.class);
        }

        assertThatThrownBy(() -> executor.executeChat(() -> "not called"))
                .isInstanceOf(CallNotPermittedException.class);
        assertThat(attempts).hasValue(2);
    }

    private AiResilienceExecutor executor(int maxAttempts, int minimumCalls) {
        return new AiResilienceExecutor(new AiResilienceProperties(
                maxAttempts,
                Duration.ZERO,
                50,
                Math.max(2, minimumCalls),
                minimumCalls,
                Duration.ofSeconds(30)
        ));
    }
}
