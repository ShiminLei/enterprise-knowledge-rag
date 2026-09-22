package com.aishare.knowledgerag.streaming;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "rag.streaming")
public record AnswerStreamingProperties(
        Duration timeout,
        int coreThreads,
        int maxThreads,
        int queueCapacity,
        int maxActiveStreams
) {
    public AnswerStreamingProperties {
        if (timeout == null || timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("rag.streaming.timeout 必须大于 0");
        }
        if (coreThreads < 1 || maxThreads < coreThreads) {
            throw new IllegalArgumentException("流式线程数配置不正确");
        }
        if (queueCapacity < 1) {
            throw new IllegalArgumentException("rag.streaming.queue-capacity 必须大于 0");
        }
        if (maxActiveStreams < 1) {
            throw new IllegalArgumentException("rag.streaming.max-active-streams 必须大于 0");
        }
    }
}
