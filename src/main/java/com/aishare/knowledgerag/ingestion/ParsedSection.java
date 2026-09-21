package com.aishare.knowledgerag.ingestion;

import java.util.List;

public record ParsedSection(
        String titlePath,
        Integer pageNumber,
        List<String> paragraphs
) {
    public ParsedSection {
        paragraphs = List.copyOf(paragraphs);
    }
}
