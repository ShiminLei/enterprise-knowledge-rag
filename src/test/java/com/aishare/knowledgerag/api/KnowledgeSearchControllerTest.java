package com.aishare.knowledgerag.api;

import com.aishare.knowledgerag.common.ApiExceptionHandler;
import com.aishare.knowledgerag.retrieval.RetrievalProperties;
import com.aishare.knowledgerag.retrieval.HybridSearchService;
import com.aishare.knowledgerag.retrieval.VectorSearchService;
import com.aishare.knowledgerag.security.AccessContext;
import com.aishare.knowledgerag.security.AccessContextService;
import com.aishare.knowledgerag.security.PermissionLevel;
import com.aishare.knowledgerag.security.AuthenticatedIdentity;
import com.aishare.knowledgerag.security.CurrentAuthenticatedIdentityProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class KnowledgeSearchControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        VectorSearchService service = mock(VectorSearchService.class);
        HybridSearchService hybridSearchService = mock(HybridSearchService.class);
        AccessContextService accessContextService = mock(AccessContextService.class);
        CurrentAuthenticatedIdentityProvider identityProvider =
                mock(CurrentAuthenticatedIdentityProvider.class);
        when(identityProvider.current()).thenReturn(new AuthenticatedIdentity(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "zhangsan"
        ));
        when(service.search(any())).thenReturn(List.of());
        when(hybridSearchService.search(any())).thenReturn(List.of());
        when(accessContextService.resolve(any(), any())).thenReturn(new AccessContext(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "zhangsan",
                Set.of("信息技术部"),
                PermissionLevel.INTERNAL
        ));
        mockMvc = MockMvcBuilders.standaloneSetup(new KnowledgeSearchController(
                        service,
                        hybridSearchService,
                        new RetrievalProperties(20, 20, 5, 0.35, 60),
                        accessContextService,
                        identityProvider
                ))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void searchesWithDefaultRetrievalSettings() throws Exception {
        mockMvc.perform(post("/api/v1/search/vector")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "question": "如何登录 VPN？"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.question").value("如何登录 VPN？"))
                .andExpect(jsonPath("$.count").value(0))
                .andExpect(jsonPath("$.results").isArray());
    }

    @Test
    void rejectsInvalidTopK() throws Exception {
        mockMvc.perform(post("/api/v1/search/vector")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "question": "如何登录 VPN？",
                                  "topK": 100
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void performsHybridSearch() throws Exception {
        mockMvc.perform(post("/api/v1/search/hybrid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "question": "VPN 错误码 720"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.question").value("VPN 错误码 720"))
                .andExpect(jsonPath("$.count").value(0));
    }
}
