package com.aishare.knowledgerag.chunking;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChunkingPropertiesTest {

    @Test
    void rejectsInvalidChunkConfiguration() {
        assertThatThrownBy(() -> new ChunkingProperties(99, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不能小于 100");
        assertThatThrownBy(() -> new ChunkingProperties(100, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不能小于 0");
        assertThatThrownBy(() -> new ChunkingProperties(100, 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("必须小于");
    }
}
