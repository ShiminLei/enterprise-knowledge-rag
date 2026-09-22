package com.aishare.knowledgerag.api;

import com.aishare.knowledgerag.common.ApiExceptionHandler;
import com.aishare.knowledgerag.prompt.PromptTemplate;
import com.aishare.knowledgerag.prompt.PromptTemplateService;
import com.aishare.knowledgerag.security.AuthenticatedIdentity;
import com.aishare.knowledgerag.security.CurrentAuthenticatedIdentityProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PromptManagementControllerTest {

    private MockMvc mockMvc;
    private PromptTemplateService promptService;

    @BeforeEach
    void setUp() {
        promptService = mock(PromptTemplateService.class);
        CurrentAuthenticatedIdentityProvider identityProvider =
                mock(CurrentAuthenticatedIdentityProvider.class);
        when(identityProvider.current()).thenReturn(new AuthenticatedIdentity(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "admin-user"
        ));
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new PromptManagementController(promptService, identityProvider)
                )
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void listsPromptVersions() throws Exception {
        when(promptService.ragAnswerPromptVersions())
                .thenReturn(List.of(template("v2", false), template("v1", true)));

        mockMvc.perform(get("/api/v1/admin/prompts/rag-answer-system/versions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].version").value("v2"))
                .andExpect(jsonPath("$[1].active").value(true));
    }

    @Test
    void createsInactivePromptVersionForAuthenticatedOperator() throws Exception {
        when(promptService.createRagAnswerPromptVersion(
                "v2", "新的系统提示词", "admin-user"
        )).thenReturn(template("v2", false));

        mockMvc.perform(post("/api/v1/admin/prompts/rag-answer-system/versions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "version": "v2",
                                  "content": "新的系统提示词"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.version").value("v2"))
                .andExpect(jsonPath("$.active").value(false));

        verify(promptService).createRagAnswerPromptVersion(
                "v2", "新的系统提示词", "admin-user"
        );
    }

    @Test
    void activatesSelectedVersion() throws Exception {
        when(promptService.activateRagAnswerPromptVersion("v1"))
                .thenReturn(template("v1", true));

        mockMvc.perform(put(
                        "/api/v1/admin/prompts/rag-answer-system/versions/v1/activate"
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value("v1"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void rejectsInvalidVersionName() throws Exception {
        mockMvc.perform(post("/api/v1/admin/prompts/rag-answer-system/versions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "version": "bad version",
                                  "content": "新的系统提示词"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    private PromptTemplate template(String version, boolean active) {
        return new PromptTemplate(
                PromptTemplateService.RAG_ANSWER_PROMPT_KEY,
                version,
                version.equals("v2") ? "新的系统提示词" : "旧的系统提示词",
                "0123456789abcdef",
                active,
                "admin-user",
                Instant.parse("2026-01-01T00:00:00Z")
        );
    }
}
