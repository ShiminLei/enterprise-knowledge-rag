package com.aishare.knowledgerag.answer;

import com.aishare.knowledgerag.document.DocumentCategory;
import com.aishare.knowledgerag.retrieval.HybridSearchResult;

import java.util.UUID;

/**
 * 可解释回答中公开的检索证据。它同时保留原始片段和各检索通道分数，
 * 便于调用方展示证据或排查召回、融合问题。
 */
public record AnswerRetrievedChunk(
        UUID chunkId,
        UUID documentId,
        int chunkIndex,
        String content,
        String titlePath,
        Integer pageNumber,
        Integer startParagraphNumber,
        Integer endParagraphNumber,
        DocumentCategory category,
        String documentVersion,
        String source,
        Double vectorScore,
        Double keywordScore,
        double fusionScore,
        double rerankScore
) {
    public static AnswerRetrievedChunk from(HybridSearchResult result) {
        var chunk = result.chunk();
        return new AnswerRetrievedChunk(
                chunk.chunkId(),
                chunk.documentId(),
                chunk.chunkIndex(),
                chunk.content(),
                chunk.titlePath(),
                chunk.pageNumber(),
                chunk.startParagraphNumber(),
                chunk.endParagraphNumber(),
                chunk.category(),
                chunk.documentVersion(),
                chunk.source(),
                result.vectorScore(),
                result.keywordScore(),
                result.rrfScore(),
                result.rerankScore()
        );
    }
}
