package com.aishare.knowledgerag.config;

import com.aishare.knowledgerag.retrieval.RetrievalProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

@Component
public class NacosRetrievalConfigParser {

    public RetrievalSettings parse(String content, RetrievalProperties current) {
        Properties values = new Properties();
        try {
            values.load(new StringReader(content == null ? "" : content));
        } catch (IOException exception) {
            throw new IllegalArgumentException("无法解析 Nacos 检索配置", exception);
        }
        return new RetrievalSettings(
                integer(values, "rag.retrieval.vector-top-k", current.vectorTopK()),
                integer(values, "rag.retrieval.keyword-top-k", current.keywordTopK()),
                integer(values, "rag.retrieval.final-top-k", current.finalTopK()),
                decimal(values, "rag.retrieval.min-score", current.minScore()),
                integer(values, "rag.retrieval.rrf-k", current.rrfK())
        );
    }

    private int integer(Properties values, String key, int fallback) {
        String value = values.getProperty(key);
        return value == null || value.isBlank() ? fallback : Integer.parseInt(value.strip());
    }

    private double decimal(Properties values, String key, double fallback) {
        String value = values.getProperty(key);
        return value == null || value.isBlank() ? fallback : Double.parseDouble(value.strip());
    }

    public record RetrievalSettings(
            int vectorTopK,
            int keywordTopK,
            int finalTopK,
            double minScore,
            int rrfK
    ) {
        public void applyTo(RetrievalProperties properties) {
            properties.apply(vectorTopK, keywordTopK, finalTopK, minScore, rrfK);
        }
    }
}
