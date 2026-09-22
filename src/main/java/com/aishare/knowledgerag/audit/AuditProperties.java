package com.aishare.knowledgerag.audit;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rag.audit")
public record AuditProperties(String modelName, String promptVersion) {
}
