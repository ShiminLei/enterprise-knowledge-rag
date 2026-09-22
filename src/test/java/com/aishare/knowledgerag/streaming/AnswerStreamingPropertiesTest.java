package com.aishare.knowledgerag.streaming;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnswerStreamingPropertiesTest {

    @Test
    void rejectsInvalidThreadPoolSize() {
        assertThatThrownBy(() -> new AnswerStreamingProperties(
                Duration.ofSeconds(30), 24, 4, 2, 50
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("流式线程数配置不正确");
    }

    @Test
    void rejectsInvalidChunkSize() {
        assertThatThrownBy(() -> new AnswerStreamingProperties(
                Duration.ofSeconds(30), 0, 2, 8, 50
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chunk-characters");
    }
}
