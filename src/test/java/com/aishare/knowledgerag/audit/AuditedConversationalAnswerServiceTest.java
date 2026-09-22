package com.aishare.knowledgerag.audit;

import com.aishare.knowledgerag.answer.AnswerCitation;
import com.aishare.knowledgerag.answer.ChatGenerationException;
import com.aishare.knowledgerag.conversation.ConversationAnswer;
import com.aishare.knowledgerag.conversation.ConversationalAnswerService;
import com.aishare.knowledgerag.ingestion.DocumentChecksumService;
import com.aishare.knowledgerag.retrieval.VectorSearchQuery;
import com.aishare.knowledgerag.security.AccessContext;
import com.aishare.knowledgerag.security.PermissionLevel;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditedConversationalAnswerServiceTest {

    @Mock
    private ConversationalAnswerService delegate;
    @Mock
    private AuditPersistenceService persistenceService;
    @Mock
    private RequestIdProvider requestIdProvider;

    private SimpleMeterRegistry meterRegistry;
    private AuditedConversationalAnswerService service;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        service = new AuditedConversationalAnswerService(
                delegate,
                persistenceService,
                new DocumentChecksumService(),
                requestIdProvider,
                new AuditProperties("gpt-test", "v1"),
                meterRegistry
        );
        when(requestIdProvider.currentOrCreate()).thenReturn("req-test-1");
    }

    @Test
    void recordsPrivacySafeSuccessAuditAndMetrics() {
        UUID conversationId = UUID.fromString("30000000-0000-0000-0000-000000000001");
        UUID documentId = UUID.fromString("20000000-0000-0000-0000-000000000001");
        when(delegate.answer(Optional.empty(), query())).thenReturn(new ConversationAnswer(
                conversationId,
                "答案。[1]",
                true,
                1,
                List.of(new AnswerCitation(
                        1,
                        UUID.randomUUID(),
                        documentId,
                        "VPN 手册",
                        3,
                        "IT 服务台",
                        "2.3"
                ))
        ));

        service.answer(Optional.empty(), query());

        ArgumentCaptor<RagRequestAudit> audit = ArgumentCaptor.forClass(RagRequestAudit.class);
        verify(persistenceService).save(audit.capture());
        assertThat(audit.getValue().requestId()).isEqualTo("req-test-1");
        assertThat(audit.getValue().questionDigest()).hasSize(64).doesNotContain("VPN");
        assertThat(audit.getValue().retrievedDocumentIds()).containsExactly(documentId);
        assertThat(audit.getValue().outcome()).isEqualTo("SUCCESS");
        assertThat(meterRegistry.counter("rag.answer.requests", "outcome", "SUCCESS").count())
                .isEqualTo(1);
    }

    @Test
    void recordsFailureThenRethrowsOriginalException() {
        ChatGenerationException failure = new ChatGenerationException("模型失败");
        when(delegate.answer(Optional.empty(), query())).thenThrow(failure);

        assertThatThrownBy(() -> service.answer(Optional.empty(), query()))
                .isSameAs(failure);

        ArgumentCaptor<RagRequestAudit> audit = ArgumentCaptor.forClass(RagRequestAudit.class);
        verify(persistenceService).save(audit.capture());
        assertThat(audit.getValue().outcome()).isEqualTo("FAILED");
        assertThat(audit.getValue().errorCode()).isEqualTo("CHAT_GENERATION_FAILED");
    }

    @Test
    void auditFailureDoesNotBreakSuccessfulAnswer() {
        ConversationAnswer expected = new ConversationAnswer(
                UUID.randomUUID(), "答案", false, 0, List.of()
        );
        when(delegate.answer(Optional.empty(), query())).thenReturn(expected);
        doThrow(new IllegalStateException("audit down"))
                .when(persistenceService).save(any());

        assertThat(service.answer(Optional.empty(), query())).isEqualTo(expected);
    }

    private VectorSearchQuery query() {
        return new VectorSearchQuery(
                "如何登录 VPN？",
                new AccessContext(
                        UUID.fromString("00000000-0000-0000-0000-000000000001"),
                        "zhangsan",
                        Set.of("信息技术部"),
                        PermissionLevel.INTERNAL
                ),
                null,
                5,
                0.35
        );
    }
}
