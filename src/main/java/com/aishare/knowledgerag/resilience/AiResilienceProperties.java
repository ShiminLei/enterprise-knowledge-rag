package com.aishare.knowledgerag.resilience;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "rag.ai-resilience")
public record AiResilienceProperties(
        int maxAttempts,
        Duration retryWait,
        float failureRateThreshold,
        int slidingWindowSize,
        int minimumNumberOfCalls,
        Duration openStateWait
) {
    public AiResilienceProperties {
        if (maxAttempts < 1 || maxAttempts > 5) {
            throw new IllegalArgumentException("rag.ai-resilience.max-attempts 必须在 1 到 5 之间");
        }
        if (retryWait == null || retryWait.isNegative()) {
            throw new IllegalArgumentException("rag.ai-resilience.retry-wait 不能为负数");
        }
        if (failureRateThreshold <= 0 || failureRateThreshold > 100) {
            throw new IllegalArgumentException("熔断失败率必须在 0 到 100 之间");
        }
        if (slidingWindowSize < 2 || minimumNumberOfCalls < 1
                || minimumNumberOfCalls > slidingWindowSize) {
            throw new IllegalArgumentException("熔断窗口配置不正确");
        }
        if (openStateWait == null || openStateWait.isNegative() || openStateWait.isZero()) {
            throw new IllegalArgumentException("熔断恢复等待时间必须大于 0");
        }
    }
}
