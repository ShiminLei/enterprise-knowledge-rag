package com.aishare.knowledgerag.ingestion;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DocumentParserRegistry {

    private final List<DocumentParser> parsers;

    public DocumentParserRegistry(List<DocumentParser> parsers) {
        this.parsers = List.copyOf(parsers);
    }

    public DocumentParser requireParser(String fileName, String mediaType) {
        return parsers.stream()
                .filter(parser -> parser.supports(fileName, mediaType))
                .findFirst()
                .orElseThrow(() -> new DocumentParseException(
                        "没有可用的文档解析器: fileName=%s, mediaType=%s"
                                .formatted(fileName, mediaType)
                ));
    }
}
