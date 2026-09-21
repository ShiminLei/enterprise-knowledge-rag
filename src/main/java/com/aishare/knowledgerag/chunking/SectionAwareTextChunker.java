package com.aishare.knowledgerag.chunking;

import com.aishare.knowledgerag.ingestion.ParsedDocument;
import com.aishare.knowledgerag.ingestion.ParsedSection;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class SectionAwareTextChunker implements TextChunker {

    private static final double MINIMUM_BOUNDARY_RATIO = 0.6;
    private static final String NATURAL_BOUNDARIES = "\n。！？；.!?;，, ";

    private final ChunkingProperties properties;

    public SectionAwareTextChunker(ChunkingProperties properties) {
        this.properties = properties;
    }

    @Override
    public List<ChunkCandidate> chunk(ParsedDocument document) {
        Objects.requireNonNull(document, "document 不能为 null");

        List<ChunkCandidate> chunks = new ArrayList<>();
        int nextChunkIndex = 0;
        int paragraphOffset = 0;

        for (ParsedSection section : document.sections()) {
            SectionText sectionText = buildSectionText(section, paragraphOffset);
            if (!sectionText.text().isBlank()) {
                List<ChunkCandidate> sectionChunks = chunkSection(
                        section,
                        sectionText,
                        nextChunkIndex
                );
                chunks.addAll(sectionChunks);
                nextChunkIndex += sectionChunks.size();
            }
            paragraphOffset += section.paragraphs().size();
        }

        return List.copyOf(chunks);
    }

    private List<ChunkCandidate> chunkSection(
            ParsedSection section,
            SectionText sectionText,
            int firstChunkIndex
    ) {
        List<ChunkCandidate> chunks = new ArrayList<>();
        int cursor = 0;

        while (cursor < sectionText.text().length()) {
            cursor = skipWhitespace(sectionText.text(), cursor);
            if (cursor >= sectionText.text().length()) {
                break;
            }

            int maximumEnd = Math.min(cursor + properties.size(), sectionText.text().length());
            int end = maximumEnd == sectionText.text().length()
                    ? maximumEnd
                    : findNaturalBoundary(sectionText.text(), cursor, maximumEnd);
            if (end <= cursor) {
                end = maximumEnd;
            }

            String content = sectionText.text().substring(cursor, end).strip();
            if (!content.isBlank()) {
                int contentStart = findContentStart(sectionText.text(), cursor, end);
                int contentEnd = findContentEnd(sectionText.text(), contentStart, end);
                ParagraphRange range = findParagraphRange(
                        sectionText.paragraphSpans(),
                        contentStart,
                        contentEnd
                );
                chunks.add(new ChunkCandidate(
                        firstChunkIndex + chunks.size(),
                        content,
                        section.titlePath(),
                        section.pageNumber(),
                        range.startParagraphNumber(),
                        range.endParagraphNumber()
                ));
            }

            if (end >= sectionText.text().length()) {
                break;
            }
            int nextCursor = Math.max(end - properties.overlap(), cursor + 1);
            cursor = skipWhitespace(sectionText.text(), nextCursor);
        }

        return chunks;
    }

    private SectionText buildSectionText(ParsedSection section, int paragraphOffset) {
        StringBuilder text = new StringBuilder();
        List<ParagraphSpan> spans = new ArrayList<>();

        for (int index = 0; index < section.paragraphs().size(); index++) {
            String paragraph = section.paragraphs().get(index).strip();
            if (paragraph.isBlank()) {
                continue;
            }
            if (!text.isEmpty()) {
                text.append("\n\n");
            }
            int start = text.length();
            text.append(paragraph);
            int end = text.length();
            spans.add(new ParagraphSpan(
                    start,
                    end,
                    paragraphOffset + index + 1
            ));
        }

        return new SectionText(text.toString(), List.copyOf(spans));
    }

    private int findNaturalBoundary(String text, int start, int maximumEnd) {
        int minimumEnd = start + (int) Math.floor(properties.size() * MINIMUM_BOUNDARY_RATIO);
        for (int index = maximumEnd; index > minimumEnd; index--) {
            char previousCharacter = text.charAt(index - 1);
            if (NATURAL_BOUNDARIES.indexOf(previousCharacter) >= 0) {
                return index;
            }
        }
        return maximumEnd;
    }

    private ParagraphRange findParagraphRange(
            List<ParagraphSpan> spans,
            int contentStart,
            int contentEnd
    ) {
        int startParagraph = spans.isEmpty() ? 0 : spans.get(0).paragraphNumber();
        int endParagraph = startParagraph;

        for (ParagraphSpan span : spans) {
            if (span.end() > contentStart) {
                startParagraph = span.paragraphNumber();
                break;
            }
        }
        for (ParagraphSpan span : spans) {
            if (span.start() < contentEnd) {
                endParagraph = span.paragraphNumber();
            } else {
                break;
            }
        }
        return new ParagraphRange(startParagraph, endParagraph);
    }

    private int skipWhitespace(String text, int offset) {
        int result = offset;
        while (result < text.length() && Character.isWhitespace(text.charAt(result))) {
            result++;
        }
        return result;
    }

    private int findContentStart(String text, int start, int end) {
        int result = start;
        while (result < end && Character.isWhitespace(text.charAt(result))) {
            result++;
        }
        return result;
    }

    private int findContentEnd(String text, int start, int end) {
        int result = end;
        while (result > start && Character.isWhitespace(text.charAt(result - 1))) {
            result--;
        }
        return result;
    }

    private record SectionText(String text, List<ParagraphSpan> paragraphSpans) {
    }

    private record ParagraphSpan(int start, int end, int paragraphNumber) {
    }

    private record ParagraphRange(int startParagraphNumber, int endParagraphNumber) {
    }
}
