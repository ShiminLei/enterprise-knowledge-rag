package com.aishare.knowledgerag.retrieval;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * 对 RRF 候选做第二阶段业务重排：优先两路共同命中的证据，并以原始语义、
 * 关键词分数打破接近的 RRF 排名。权重刻意小于双路奖励，避免覆盖召回排序。
 */
@Component
public class HybridResultReranker {

    private static final double DUAL_CHANNEL_BOOST = 0.01;
    private static final double SOURCE_SCORE_WEIGHT = 0.001;

    public List<HybridSearchResult> rerank(List<HybridSearchResult> candidates) {
        return candidates.stream()
                .map(candidate -> candidate.withRerankScore(score(candidate)))
                .sorted(Comparator
                        .comparingDouble(HybridSearchResult::rerankScore)
                        .reversed()
                        .thenComparing(result -> result.chunk().chunkId()))
                .toList();
    }

    private double score(HybridSearchResult candidate) {
        boolean dualChannel = candidate.vectorRank() != null
                && candidate.keywordRank() != null;
        return candidate.rrfScore()
                + (dualChannel ? DUAL_CHANNEL_BOOST : 0)
                + SOURCE_SCORE_WEIGHT * value(candidate.vectorScore())
                + SOURCE_SCORE_WEIGHT * value(candidate.keywordScore());
    }

    private double value(Double score) {
        return score == null ? 0 : score;
    }
}
