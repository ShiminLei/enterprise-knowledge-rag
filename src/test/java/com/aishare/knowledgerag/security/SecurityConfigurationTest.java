package com.aishare.knowledgerag.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.aishare.knowledgerag.api.AdminPageController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitConfig(SecurityConfigurationTest.TestConfiguration.class)
@WebAppConfiguration
class SecurityConfigurationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void rejectsApiRequestWithoutBearerToken() throws Exception {
        mockMvc.perform(post("/api/v1/search/vector")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void allowsAuthenticatedUserToSearch() throws Exception {
        mockMvc.perform(post("/api/v1/search/vector")
                        .with(jwt().jwt(token -> token.subject("zhangsan")
                                .claim("tenant_id", "00000000-0000-0000-0000-000000000001"))))
                .andExpect(status().isOk());
    }

    @Test
    void requiresKnowledgeWriteScopeForImport() throws Exception {
        mockMvc.perform(post("/api/v1/documents/import")
                        .with(jwt()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(post("/api/v1/documents/import")
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("SCOPE_knowledge.write"))))
                .andExpect(status().isOk());
    }

    @Test
    void requiresPromptManageScopeForPromptAdministration() throws Exception {
        mockMvc.perform(get("/api/v1/admin/prompts/rag-answer-system/versions")
                        .with(jwt()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(get("/api/v1/admin/prompts/rag-answer-system/versions")
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("SCOPE_prompt.manage"))))
                .andExpect(status().isOk());
    }

    @Test
    void requiresEvaluationRunScopeForOfflineEvaluation() throws Exception {
        mockMvc.perform(post("/api/v1/admin/evaluations/retrieval-runs")
                        .with(jwt()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(post("/api/v1/admin/evaluations/retrieval-runs")
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("SCOPE_evaluation.run"))))
                .andExpect(status().isOk());
    }

    @Test
    void leavesHealthEndpointPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void allowsOpeningAdminShellWithoutExposingApiData() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection());
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({SecurityConfiguration.class, SecurityErrorWriter.class,
            AdminPageController.class, TestController.class})
    static class TestConfiguration {

        @Bean
        JwtSecurityProperties jwtSecurityProperties() {
            return new JwtSecurityProperties(
                    "enterprise-knowledge-rag",
                    "test-only-secret-with-at-least-32-bytes"
            );
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }
    }

    @RestController
    static class TestController {

        @PostMapping("/api/v1/search/vector")
        String search() {
            return "ok";
        }

        @PostMapping("/api/v1/documents/import")
        String importDocument() {
            return "ok";
        }

        @GetMapping("/actuator/health")
        String health() {
            return "UP";
        }

        @GetMapping("/api/v1/admin/prompts/rag-answer-system/versions")
        String promptVersions() {
            return "ok";
        }

        @PostMapping("/api/v1/admin/evaluations/retrieval-runs")
        String runEvaluation() {
            return "ok";
        }
    }
}
