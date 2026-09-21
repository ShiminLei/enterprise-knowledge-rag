package com.aishare.knowledgerag.ingestion;

import java.util.List;

public record ParsedDocument(
        String title,
        String sourceFileName,
        String mediaType,
        List<ParsedSection> sections
) {
    public ParsedDocument {
        sections = List.copyOf(sections);
    }

    public String fullText() {
        return sections.stream()
                .flatMap(section -> section.paragraphs().stream())
                .reduce((left, right) -> left + "\n\n" + right)
                .orElse("");
    }
}
