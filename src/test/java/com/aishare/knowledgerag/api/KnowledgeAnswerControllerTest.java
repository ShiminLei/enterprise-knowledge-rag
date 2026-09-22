package com.aishare.knowledgerag.api;

import com.aishare.knowledgerag.common.ApiExceptionHandler;
import com.aishare.knowledgerag.conversation.ConversationAnswer;
import com.aishare.knowledgerag.conversation.ConversationalAnswerService;
import com.aishare.knowledgerag.retrieval.RetrievalProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class KnowledgeAnswerControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ConversationalAnswerService answerService = mock(ConversationalAnswerService.class);
        when(answerService.answer(any(), any())).thenReturn(new ConversationAnswer(
                UUID.fromString("30000000-0000-0000-0000-000000000001"),
                "请使用公司账号登录。[1]",
                true,
                1,
                List.of()
        ));
        mockMvc = MockMvcBuilders.standaloneSetup(new KnowledgeAnswerController(
                        answerService,
                        new RetrievalProperties(20, 20, 5, 0.35, 60)
                ))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void answersQuestionUsingRagPipeline() throws Exception {
        mockMvc.perform(post("/api/v1/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "question": "如何登录 VPN？",
                                  "tenantId": "00000000-0000-0000-0000-000000000001",
                                  "userId": "zhangsan",
                                  "departments": ["信息技术部"],
                                  "permissionLevel": "INTERNAL"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationId")
                        .value("30000000-0000-0000-0000-000000000001"))
                .andExpect(jsonPath("$.answer").value("请使用公司账号登录。[1]"))
                .andExpect(jsonPath("$.grounded").value(true))
                .andExpect(jsonPath("$.retrievedCount").value(1));
    }
}
