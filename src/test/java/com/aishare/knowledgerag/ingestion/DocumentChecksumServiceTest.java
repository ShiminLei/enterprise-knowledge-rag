package com.aishare.knowledgerag.ingestion;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentChecksumServiceTest {

    private final DocumentChecksumService service = new DocumentChecksumService();

    @Test
    void calculatesStableLowercaseSha256() {
        assertThat(service.sha256("hello".getBytes(StandardCharsets.UTF_8)))
                .isEqualTo("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824");
    }
}
