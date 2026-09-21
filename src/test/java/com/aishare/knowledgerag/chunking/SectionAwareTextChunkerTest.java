package com.aishare.knowledgerag.chunking;

import com.aishare.knowledgerag.ingestion.ParsedDocument;
import com.aishare.knowledgerag.ingestion.ParsedSection;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SectionAwareTextChunkerTest {

    private final SectionAwareTextChunker chunker =
            new SectionAwareTextChunker(new ChunkingProperties(120, 20));

    @Test
    void keepsChunksWithinLimitAndAddsOverlap() {
        String paragraph = "第一句用于描述差旅报销规则。第二句补充审批要求。第三句说明超标费用处理。"
                .repeat(6);
        ParsedDocument document = document(List.of(
                new ParsedSection("差旅制度 > 住宿费", 3, List.of(paragraph))
        ));

        List<ChunkCandidate> chunks = chunker.chunk(document);

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks).allSatisfy(chunk -> {
            assertThat(chunk.characterCount()).isLessThanOrEqualTo(120);
            assertThat(chunk.titlePath()).isEqualTo("差旅制度 > 住宿费");
            assertThat(chunk.pageNumber()).isEqualTo(3);
            assertThat(chunk.startParagraphNumber()).isEqualTo(1);
            assertThat(chunk.endParagraphNumber()).isEqualTo(1);
        });
        assertThat(chunks).extracting(ChunkCandidate::chunkIndex)
                .containsExactlyElementsOf(java.util.stream.IntStream.range(0, chunks.size()).boxed().toList());
        assertThat(hasCommonText(chunks.get(0).content(), chunks.get(1).content(), 8)).isTrue();
    }

    @Test
    void doesNotMixSectionsAndTracksGlobalParagraphNumbers() {
        ParsedDocument document = document(List.of(
                new ParsedSection("VPN > 登录", null, List.of("登录说明。", "动态口令说明。")),
                new ParsedSection("VPN > 故障", 5, List.of("故障处理说明。"))
        ));

        List<ChunkCandidate> chunks = chunker.chunk(document);

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).titlePath()).isEqualTo("VPN > 登录");
        assertThat(chunks.get(0).startParagraphNumber()).isEqualTo(1);
        assertThat(chunks.get(0).endParagraphNumber()).isEqualTo(2);
        assertThat(chunks.get(1).titlePath()).isEqualTo("VPN > 故障");
        assertThat(chunks.get(1).pageNumber()).isEqualTo(5);
        assertThat(chunks.get(1).startParagraphNumber()).isEqualTo(3);
        assertThat(chunks.get(1).endParagraphNumber()).isEqualTo(3);
    }

    @Test
    void rejectsNullDocument() {
        assertThatThrownBy(() -> chunker.chunk(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("document 不能为 null");
    }

    private ParsedDocument document(List<ParsedSection> sections) {
        return new ParsedDocument("测试文档", "test.md", "text/markdown", sections);
    }

    private boolean hasCommonText(String left, String right, int minimumLength) {
        for (int start = 0; start <= left.length() - minimumLength; start++) {
            String candidate = left.substring(start, start + minimumLength);
            if (right.contains(candidate)) {
                return true;
            }
        }
        return false;
    }
}
