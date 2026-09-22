package com.aishare.knowledgerag.prompt;

import com.aishare.knowledgerag.ingestion.DocumentChecksumService;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import org.mockito.ArgumentCaptor;

class PromptTemplateServiceTest {

    @Test
    void returnsActiveRagAnswerPrompt() {
        PromptTemplate expected = new PromptTemplate(
                PromptTemplateService.RAG_ANSWER_PROMPT_KEY,
                "v2",
                "只根据资料回答",
                "checksum"
        );
        PromptTemplateRepository repository = mock(PromptTemplateRepository.class);
        when(repository.findActive(PromptTemplateService.RAG_ANSWER_PROMPT_KEY))
                .thenReturn(Optional.of(expected));
        PromptTemplateService service = new PromptTemplateService(
                repository, new DocumentChecksumService());

        assertThat(service.activeRagAnswerPrompt()).isEqualTo(expected);
    }

    @Test
    void failsClosedWhenNoPromptIsActive() {
        PromptTemplateRepository repository = mock(PromptTemplateRepository.class);
        when(repository.findActive(PromptTemplateService.RAG_ANSWER_PROMPT_KEY))
                .thenReturn(Optional.empty());
        PromptTemplateService service = new PromptTemplateService(
                repository, new DocumentChecksumService());

        assertThatThrownBy(service::activeRagAnswerPrompt)
                .isInstanceOf(PromptTemplateNotFoundException.class)
                .hasMessageContaining(PromptTemplateService.RAG_ANSWER_PROMPT_KEY);
    }

    @Test
    void createsInactiveVersionWithChecksumAndCreator() {
        PromptTemplateRepository repository = mock(PromptTemplateRepository.class);
        when(repository.findByVersion(
                PromptTemplateService.RAG_ANSWER_PROMPT_KEY, "v2"
        )).thenReturn(Optional.empty());
        PromptTemplateService service = new PromptTemplateService(
                repository, new DocumentChecksumService());

        PromptTemplate created = service.createRagAnswerPromptVersion(
                "v2", "新的系统提示词", "admin-user"
        );

        verify(repository).lockVersions(PromptTemplateService.RAG_ANSWER_PROMPT_KEY);
        ArgumentCaptor<PromptTemplate> inserted =
                ArgumentCaptor.forClass(PromptTemplate.class);
        verify(repository).insert(inserted.capture());
        assertThat(created).isEqualTo(inserted.getValue());
        assertThat(created.active()).isFalse();
        assertThat(created.createdBy()).isEqualTo("admin-user");
        assertThat(created.checksum()).hasSize(64);
    }

    @Test
    void activatesExistingVersionAndReturnsRefreshedState() {
        PromptTemplateRepository repository = mock(PromptTemplateRepository.class);
        PromptTemplate inactive = template("v2", false);
        PromptTemplate active = template("v2", true);
        when(repository.findByVersion(
                PromptTemplateService.RAG_ANSWER_PROMPT_KEY, "v2"
        )).thenReturn(Optional.of(inactive), Optional.of(active));
        when(repository.activate(
                PromptTemplateService.RAG_ANSWER_PROMPT_KEY, "v2"
        )).thenReturn(1);
        PromptTemplateService service = new PromptTemplateService(
                repository, new DocumentChecksumService());

        assertThat(service.activateRagAnswerPromptVersion("v2")).isEqualTo(active);

        verify(repository).lockVersions(PromptTemplateService.RAG_ANSWER_PROMPT_KEY);
        verify(repository).deactivateAll(PromptTemplateService.RAG_ANSWER_PROMPT_KEY);
        verify(repository).activate(PromptTemplateService.RAG_ANSWER_PROMPT_KEY, "v2");
    }

    @Test
    void rejectsDuplicateVersion() {
        PromptTemplateRepository repository = mock(PromptTemplateRepository.class);
        when(repository.findByVersion(
                PromptTemplateService.RAG_ANSWER_PROMPT_KEY, "v1"
        )).thenReturn(Optional.of(template("v1", true)));
        PromptTemplateService service = new PromptTemplateService(
                repository, new DocumentChecksumService());

        assertThatThrownBy(() -> service.createRagAnswerPromptVersion(
                "v1", "重复内容", "admin-user"
        )).isInstanceOf(PromptVersionConflictException.class);
    }

    private PromptTemplate template(String version, boolean active) {
        return new PromptTemplate(
                PromptTemplateService.RAG_ANSWER_PROMPT_KEY,
                version,
                "提示词",
                "checksum",
                active,
                "admin-user",
                Instant.parse("2026-01-01T00:00:00Z")
        );
    }
}
