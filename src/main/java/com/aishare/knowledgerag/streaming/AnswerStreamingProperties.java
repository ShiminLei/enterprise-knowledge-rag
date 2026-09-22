package com.aishare.knowledgerag.streaming;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "rag.streaming")
public record AnswerStreamingProperties(
        Duration timeout,
        int chunkCharacters,
        int coreThreads,
        int maxThreads,
        int queueCapacity
) {
    public AnswerStreamingProperties {
        if (timeout == null || timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("rag.streaming.timeout 必须大于 0");
        }
        if (chunkCharacters < 1 || chunkCharacters > 500) {
            throw new IllegalArgumentException("rag.streaming.chunk-characters 必须在 1 到 500 之间");
        }
        if (coreThreads < 1 || maxThreads < coreThreads) {
            throw new IllegalArgumentException("流式线程数配置不正确");
        }
        if (queueCapacity < 1) {
            throw new IllegalArgumentException("rag.streaming.queue-capacity 必须大于 0");
        }
    }
}
