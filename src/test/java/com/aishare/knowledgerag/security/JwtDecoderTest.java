package com.aishare.knowledgerag.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtDecoderTest {

    private static final String SECRET =
            "test-only-secret-with-at-least-32-bytes";

    private final JwtDecoder decoder = new SecurityConfiguration().jwtDecoder(
            new JwtSecurityProperties("enterprise-knowledge-rag", SECRET)
    );

    @Test
    void verifiesSignatureIssuerAndClaims() throws Exception {
        Jwt jwt = decoder.decode(token("enterprise-knowledge-rag", SECRET));

        assertThat(jwt.getSubject()).isEqualTo("zhangsan");
        assertThat(jwt.getClaimAsString("tenant_id"))
                .isEqualTo("00000000-0000-0000-0000-000000000001");
    }

    @Test
    void rejectsTokenSignedWithDifferentSecret() throws Exception {
        String otherSecret = "different-test-secret-with-at-least-32-bytes";

        assertThatThrownBy(() -> decoder.decode(
                token("enterprise-knowledge-rag", otherSecret)))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsTokenFromDifferentIssuer() throws Exception {
        assertThatThrownBy(() -> decoder.decode(token("another-system", SECRET)))
                .isInstanceOf(JwtValidationException.class);
    }

    private String token(String issuer, String secret) throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject("zhangsan")
                .claim("tenant_id", "00000000-0000-0000-0000-000000000001")
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(300)))
                .build();
        SignedJWT signedJwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        signedJwt.sign(new MACSigner(secret.getBytes(StandardCharsets.UTF_8)));
        return signedJwt.serialize();
    }
}
