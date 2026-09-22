package com.aishare.knowledgerag.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

@ConfigurationProperties("rag.security.jwt")
public record JwtSecurityProperties(String issuer, String secret) {

    public JwtSecurityProperties {
        Assert.hasText(issuer, "rag.security.jwt.issuer 不能为空");
        Assert.hasText(secret, "必须通过 RAG_JWT_SECRET 提供 JWT 签名密钥");
        Assert.isTrue(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length >= 32,
                "RAG_JWT_SECRET 至少需要 32 字节");
    }
}
