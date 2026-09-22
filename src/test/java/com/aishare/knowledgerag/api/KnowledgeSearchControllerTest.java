package com.aishare.knowledgerag.api;

import com.aishare.knowledgerag.common.ApiExceptionHandler;
import com.aishare.knowledgerag.retrieval.RetrievalProperties;
import com.aishare.knowledgerag.retrieval.HybridSearchService;
import com.aishare.knowledgerag.retrieval.VectorSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

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
        when(service.search(any())).thenReturn(List.of());
        when(hybridSearchService.search(any())).thenReturn(List.of());
        mockMvc = MockMvcBuilders.standaloneSetup(new KnowledgeSearchController(
                        service,
                        hybridSearchService,
                        new RetrievalProperties(20, 20, 5, 0.35, 60)
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
                                  "question": "如何登录 VPN？",
                                  "tenantId": "00000000-0000-0000-0000-000000000001",
                                  "userId": "zhangsan",
                                  "departments": ["信息技术部"],
                                  "permissionLevel": "INTERNAL"
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
                                  "tenantId": "00000000-0000-0000-0000-000000000001",
                                  "userId": "zhangsan",
                                  "departments": [],
                                  "permissionLevel": "PUBLIC",
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
                                  "question": "VPN 错误码 720",
                                  "tenantId": "00000000-0000-0000-0000-000000000001",
                                  "userId": "zhangsan",
                                  "departments": ["信息技术部"],
                                  "permissionLevel": "INTERNAL"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.question").value("VPN 错误码 720"))
                .andExpect(jsonPath("$.count").value(0));
    }
}
