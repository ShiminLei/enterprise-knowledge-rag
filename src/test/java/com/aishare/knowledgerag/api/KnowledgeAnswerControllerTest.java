package com.aishare.knowledgerag.api;

import com.aishare.knowledgerag.common.ApiExceptionHandler;
import com.aishare.knowledgerag.audit.AuditedConversationalAnswerService;
import com.aishare.knowledgerag.audit.RequestIdProvider;
import com.aishare.knowledgerag.conversation.ConversationAnswer;
import com.aishare.knowledgerag.conversation.ConversationAnswerStream;
import com.aishare.knowledgerag.retrieval.RetrievalProperties;
import com.aishare.knowledgerag.security.AccessContext;
import com.aishare.knowledgerag.security.AccessContextService;
import com.aishare.knowledgerag.security.PermissionLevel;
import com.aishare.knowledgerag.security.AuthenticatedIdentity;
import com.aishare.knowledgerag.security.CurrentAuthenticatedIdentityProvider;
import com.aishare.knowledgerag.streaming.AnswerStreamingProperties;
import com.aishare.knowledgerag.streaming.AnswerStreamingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;
import java.util.Set;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import reactor.core.publisher.Flux;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;
import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeAnswerControllerTest {

    private MockMvc mockMvc;
    private ExecutorService streamingExecutor;
    private AuditedConversationalAnswerService answerService;

    @BeforeEach
    void setUp() {
        streamingExecutor = Executors.newSingleThreadExecutor();
        answerService = mock(AuditedConversationalAnswerService.class);
        AccessContextService accessContextService = mock(AccessContextService.class);
        CurrentAuthenticatedIdentityProvider identityProvider =
                mock(CurrentAuthenticatedIdentityProvider.class);
        when(identityProvider.current()).thenReturn(new AuthenticatedIdentity(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "zhangsan"
        ));
        when(accessContextService.resolve(any(), any())).thenReturn(new AccessContext(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "zhangsan",
                Set.of("信息技术部"),
                PermissionLevel.INTERNAL
        ));
        when(answerService.answer(any(), any())).thenReturn(new ConversationAnswer(
                UUID.fromString("30000000-0000-0000-0000-000000000001"),
                "请使用公司账号登录。[1]",
                true,
                1,
                List.of()
        ));
        when(answerService.stream(any(), any())).thenReturn(new ConversationAnswerStream(
                UUID.fromString("30000000-0000-0000-0000-000000000001"),
                true,
                1,
                List.of(),
                Flux.just("请使用公司", "账号登录。[1]")
        ));
        AnswerStreamingService streamingService = new AnswerStreamingService(
                answerService,
                new RequestIdProvider(),
                new AnswerStreamingProperties(Duration.ofSeconds(5), 1, 1, 1, 4),
                streamingExecutor
        );
        mockMvc = MockMvcBuilders.standaloneSetup(new KnowledgeAnswerController(
                        answerService,
                        new RetrievalProperties(20, 20, 5, 0.35, 60),
                        accessContextService,
                        identityProvider,
                        streamingService
                ))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @AfterEach
    void shutDownExecutor() {
        streamingExecutor.shutdownNow();
    }

    @Test
    void answersQuestionUsingRagPipeline() throws Exception {
        mockMvc.perform(post("/api/v1/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "question": "如何登录 VPN？"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationId")
                        .value("30000000-0000-0000-0000-000000000001"))
                .andExpect(jsonPath("$.answer").value("请使用公司账号登录。[1]"))
                .andExpect(jsonPath("$.grounded").value(true))
                .andExpect(jsonPath("$.retrievedCount").value(1));
    }

    @Test
    void streamsAnswerAsOrderedSseEvents() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/answers/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("""
                                {
                                  "question": "如何登录 VPN？"
                                }
                                """))
                .andExpect(request().asyncStarted())
                .andReturn();

        MvcResult streamed = mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(containsString("event:started")))
                .andExpect(content().string(containsString("event:delta")))
                .andExpect(content().string(containsString("event:citations")))
                .andExpect(content().string(containsString("event:completed")))
                .andReturn();

        String body = new String(
                streamed.getResponse().getContentAsByteArray(),
                StandardCharsets.UTF_8
        );
        assertThat(body).contains("请使用公司");
        assertThat(body.indexOf("event:started"))
                .isLessThan(body.indexOf("event:delta"));
        assertThat(body.indexOf("event:delta"))
                .isLessThan(body.indexOf("event:citations"));
        assertThat(body.indexOf("event:citations"))
                .isLessThan(body.indexOf("event:completed"));
    }

    @Test
    void sendsSafeErrorEventWhenAnswerGenerationFails() throws Exception {
        when(answerService.stream(any(), any()))
                .thenReturn(new ConversationAnswerStream(
                        UUID.randomUUID(),
                        true,
                        0,
                        List.of(),
                        Flux.error(new IllegalStateException("sensitive internal detail"))
                ));

        MvcResult result = mockMvc.perform(post("/api/v1/answers/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("{\"question\":\"如何登录 VPN？\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        MvcResult streamed = mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andReturn();
        String body = new String(
                streamed.getResponse().getContentAsByteArray(),
                StandardCharsets.UTF_8
        );
        assertThat(body)
                .contains("event:started")
                .contains("event:error")
                .contains("ANSWER_STREAM_FAILED")
                .doesNotContain("sensitive internal detail")
                .doesNotContain("event:completed");
    }
}
