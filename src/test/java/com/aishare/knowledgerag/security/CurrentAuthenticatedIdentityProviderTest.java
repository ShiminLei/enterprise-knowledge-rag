package com.aishare.knowledgerag.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrentAuthenticatedIdentityProviderTest {

    private final CurrentAuthenticatedIdentityProvider provider =
            new CurrentAuthenticatedIdentityProvider();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void readsTrustedIdentityFromJwtClaims() {
        setJwt("zhangsan", "00000000-0000-0000-0000-000000000001");

        AuthenticatedIdentity identity = provider.current();

        assertThat(identity.userId()).isEqualTo("zhangsan");
        assertThat(identity.tenantId()).isEqualTo(
                UUID.fromString("00000000-0000-0000-0000-000000000001"));
    }

    @Test
    void rejectsJwtWithoutTenantClaim() {
        setJwt("zhangsan", null);

        assertThatThrownBy(provider::current)
                .isInstanceOf(InvalidAuthenticatedIdentityException.class)
                .hasMessage("JWT 缺少 tenant_id 租户标识");
    }

    @Test
    void rejectsMalformedTenantClaim() {
        setJwt("zhangsan", "not-a-uuid");

        assertThatThrownBy(provider::current)
                .isInstanceOf(InvalidAuthenticatedIdentityException.class)
                .hasMessage("JWT 中的 tenant_id 不是合法 UUID");
    }

    private void setJwt(String subject, String tenantId) {
        Jwt.Builder builder = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject(subject)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300));
        if (tenantId != null) {
            builder.claim("tenant_id", tenantId);
        }
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(
                builder.build(),
                java.util.List.of(new SimpleGrantedAuthority("SCOPE_test"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
