package com.aishare.knowledgerag.ingestion;

import com.aishare.knowledgerag.chunking.ChunkCandidate;
import com.aishare.knowledgerag.chunking.TextChunker;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class DocumentIngestionPreviewService {

    private final DocumentParserRegistry parserRegistry;
    private final TextChunker textChunker;

    public DocumentIngestionPreviewService(
            DocumentParserRegistry parserRegistry,
            TextChunker textChunker
    ) {
        this.parserRegistry = parserRegistry;
        this.textChunker = textChunker;
    }

    public IngestionPreview preview(String originalFileName, String mediaType, byte[] content) {
        String safeFileName = validateAndCleanFileName(originalFileName);
        if (content == null || content.length == 0) {
            throw new DocumentParseException("上传文件为空: " + safeFileName);
        }

        DocumentParser parser = parserRegistry.requireParser(safeFileName, mediaType);
        ParsedDocument document = parser.parse(safeFileName, mediaType, content);
        List<ChunkCandidate> chunks = textChunker.chunk(document);
        int paragraphCount = document.sections().stream()
                .mapToInt(section -> section.paragraphs().size())
                .sum();

        return new IngestionPreview(
                document.title(),
                document.sourceFileName(),
                document.mediaType(),
                document.sections().size(),
                paragraphCount,
                chunks.size(),
                chunks
        );
    }

    private String validateAndCleanFileName(String originalFileName) {
        if (!StringUtils.hasText(originalFileName)) {
            throw new DocumentParseException("上传文件缺少文件名");
        }
        String safeFileName = StringUtils.cleanPath(originalFileName);
        if (safeFileName.contains("..")) {
            throw new DocumentParseException("文件名包含非法路径: " + originalFileName);
        }
        return safeFileName;
    }
}
