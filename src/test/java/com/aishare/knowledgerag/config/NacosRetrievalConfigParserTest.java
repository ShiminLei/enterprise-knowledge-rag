package com.aishare.knowledgerag.config;

import com.aishare.knowledgerag.retrieval.RetrievalProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NacosRetrievalConfigParserTest {

    @Test
    void appliesPublishedValuesAndKeepsOmittedValues() {
        RetrievalProperties properties = new RetrievalProperties(20, 20, 5, 0.5, 60);
        var settings = new NacosRetrievalConfigParser().parse("""
                rag.retrieval.final-top-k=8
                rag.retrieval.min-score=0.62
                """, properties);

        settings.applyTo(properties);

        assertThat(properties.vectorTopK()).isEqualTo(20);
        assertThat(properties.finalTopK()).isEqualTo(8);
        assertThat(properties.minScore()).isEqualTo(0.62);
    }

    @Test
    void rejectsInvalidPublishedValuesWithoutPartiallyUpdatingCurrentSnapshot() {
        RetrievalProperties properties = new RetrievalProperties(20, 20, 5, 0.5, 60);
        var settings = new NacosRetrievalConfigParser().parse(
                "rag.retrieval.min-score=2.0", properties
        );

        assertThatThrownBy(() -> settings.applyTo(properties))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(properties.minScore()).isEqualTo(0.5);
    }
}
