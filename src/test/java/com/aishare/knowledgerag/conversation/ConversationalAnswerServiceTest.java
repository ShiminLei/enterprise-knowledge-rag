package com.aishare.knowledgerag.conversation;

import com.aishare.knowledgerag.answer.GroundedAnswer;
import com.aishare.knowledgerag.answer.RagAnswerService;
import com.aishare.knowledgerag.retrieval.VectorSearchQuery;
import com.aishare.knowledgerag.security.AccessContext;
import com.aishare.knowledgerag.security.PermissionLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationalAnswerServiceTest {

    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private ConversationMessageRepository messageRepository;
    @Mock
    private ConversationExchangePersistenceService persistenceService;
    @Mock
    private RagAnswerService ragAnswerService;

    private ConversationalAnswerService service;

    @BeforeEach
    void setUp() {
        service = new ConversationalAnswerService(
                conversationRepository,
                messageRepository,
                persistenceService,
                ragAnswerService,
                new ConversationProperties(10, 4000)
        );
    }

    @Test
    void createsConversationAndPersistsFirstExchange() {
        GroundedAnswer grounded = new GroundedAnswer("答案。[1]", true, 1, List.of());
        when(ragAnswerService.answer(any(VectorSearchQuery.class), anyList()))
                .thenReturn(grounded);

        ConversationAnswer result = service.answer(Optional.empty(), query());

        assertThat(result.conversationId()).isNotNull();
        ArgumentCaptor<Conversation> conversation = ArgumentCaptor.forClass(Conversation.class);
        verify(persistenceService).save(
                conversation.capture(),
                org.mockito.ArgumentMatchers.eq(true),
                org.mockito.ArgumentMatchers.eq("如何登录 VPN？"),
                org.mockito.ArgumentMatchers.eq(grounded)
        );
        assertThat(conversation.getValue().title()).isEqualTo("如何登录 VPN？");
        verify(messageRepository, never()).findRecent(any(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void loadsOwnedConversationHistoryForFollowUpQuestion() {
        UUID conversationId = UUID.fromString("30000000-0000-0000-0000-000000000001");
        Conversation conversation = new Conversation(
                conversationId,
                query().accessContext().tenantId(),
                "zhangsan",
                "VPN",
                Instant.parse("2026-01-01T00:00:00Z")
        );
        List<ConversationTurn> history = List.of(
                new ConversationTurn(MessageRole.USER, "VPN 是什么？"),
                new ConversationTurn(MessageRole.ASSISTANT, "是一种远程连接方式。")
        );
        when(conversationRepository.findOwned(
                conversationId,
                query().accessContext().tenantId(),
                "zhangsan"
        )).thenReturn(Optional.of(conversation));
        when(messageRepository.findRecent(conversationId, 10)).thenReturn(history);
        when(ragAnswerService.answer(any(VectorSearchQuery.class), anyList()))
                .thenReturn(new GroundedAnswer("后续答案。[1]", true, 1, List.of()));

        service.answer(Optional.of(conversationId), query());

        verify(ragAnswerService).answer(any(VectorSearchQuery.class),
                org.mockito.ArgumentMatchers.eq(history));
        verify(persistenceService).save(
                org.mockito.ArgumentMatchers.eq(conversation),
                org.mockito.ArgumentMatchers.eq(false),
                org.mockito.ArgumentMatchers.anyString(),
                any(GroundedAnswer.class)
        );
    }

    @Test
    void hidesWhetherForeignConversationExists() {
        UUID conversationId = UUID.randomUUID();
        when(conversationRepository.findOwned(
                conversationId,
                query().accessContext().tenantId(),
                "zhangsan"
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.answer(Optional.of(conversationId), query()))
                .isInstanceOf(ConversationNotFoundException.class)
                .hasMessage("会话不存在或无权访问");
        verify(ragAnswerService, never()).answer(any(), anyList());
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
