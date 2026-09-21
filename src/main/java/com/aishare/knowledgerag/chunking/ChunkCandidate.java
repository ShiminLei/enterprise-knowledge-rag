package com.aishare.knowledgerag.chunking;

public record ChunkCandidate(
        int chunkIndex,
        String content,
        String titlePath,
        Integer pageNumber,
        int startParagraphNumber,
        int endParagraphNumber
) {
    public int characterCount() {
        return content.length();
    }
}
