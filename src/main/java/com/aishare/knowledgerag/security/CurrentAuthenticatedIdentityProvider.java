package com.aishare.knowledgerag.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CurrentAuthenticatedIdentityProvider {

    public AuthenticatedIdentity current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new InvalidAuthenticatedIdentityException("缺少有效的 JWT 身份");
        }

        String userId = jwt.getSubject();
        String tenantId = jwt.getClaimAsString("tenant_id");
        if (userId == null || userId.isBlank()) {
            throw new InvalidAuthenticatedIdentityException("JWT 缺少 sub 用户标识");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new InvalidAuthenticatedIdentityException("JWT 缺少 tenant_id 租户标识");
        }

        try {
            return new AuthenticatedIdentity(UUID.fromString(tenantId), userId.strip());
        } catch (IllegalArgumentException exception) {
            throw new InvalidAuthenticatedIdentityException("JWT 中的 tenant_id 不是合法 UUID");
        }
    }
}
