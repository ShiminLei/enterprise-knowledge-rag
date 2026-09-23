package com.aishare.knowledgerag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rag.nacos")
public record NacosProperties(
        boolean enabled,
        String serverAddress,
        String namespace,
        String username,
        String password,
        String dataId,
        String group,
        long timeoutMs
) {
}
