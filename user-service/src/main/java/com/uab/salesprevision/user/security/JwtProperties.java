package com.uab.salesprevision.user.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
    /** Base64-encoded PKCS8 RSA private key, used to sign tokens (RS256). */
    private String privateKey;
    /** Base64-encoded X.509 RSA public key, published via the JWKS endpoint. */
    private String publicKey;
    /** Key ID (kid) advertised in tokens and in the JWKS document. */
    private String keyId = "user-service-key-1";
    private long expirationMs = 3_600_000;
}
