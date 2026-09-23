package com.aishare.knowledgerag.retrieval;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rag.retrieval")
public class RetrievalProperties {

    private volatile int vectorTopK;
    private volatile int keywordTopK;
    private volatile int finalTopK;
    private volatile double minScore;
    private volatile int rrfK;

    public RetrievalProperties(
            int vectorTopK,
            int keywordTopK,
            int finalTopK,
            double minScore,
            int rrfK
    ) {
        apply(vectorTopK, keywordTopK, finalTopK, minScore, rrfK);
    }

    public synchronized void apply(
            int vectorTopK,
            int keywordTopK,
            int finalTopK,
            double minScore,
            int rrfK
    ) {
        if (vectorTopK <= 0 || keywordTopK <= 0 || finalTopK <= 0 || rrfK <= 0) {
            throw new IllegalArgumentException("检索数量配置必须大于 0");
        }
        if (minScore < 0 || minScore > 1) {
            throw new IllegalArgumentException("rag.retrieval.min-score 必须在 0 到 1 之间");
        }
        this.vectorTopK = vectorTopK;
        this.keywordTopK = keywordTopK;
        this.finalTopK = finalTopK;
        this.minScore = minScore;
        this.rrfK = rrfK;
    }

    public int vectorTopK() { return vectorTopK; }
    public int keywordTopK() { return keywordTopK; }
    public int finalTopK() { return finalTopK; }
    public double minScore() { return minScore; }
    public int rrfK() { return rrfK; }
}
