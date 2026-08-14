package com.uab.salesprevision.user.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.interfaces.RSAPublicKey;
import java.util.Map;

/**
 * Publishes this service's RSA public key so every resource server
 * (template-service, gateway-service, etc.) can verify tokens without ever
 * holding a shared secret or key material of their own.
 */
@RestController
@Tag(name = "JWKS", description = "Publishes the RSA public key used to verify tokens issued by this service.")
public class JwksController {

    private final Map<String, Object> jwkSet;

    public JwksController(JwtProperties props) {
        RSAPublicKey publicKey = RsaKeyUtils.parsePublicKey(props.getPublicKey());
        RSAKey jwk = new RSAKey.Builder(publicKey)
                .keyUse(KeyUse.SIGNATURE)
                .keyID(props.getKeyId())
                .algorithm(com.nimbusds.jose.JWSAlgorithm.RS256)
                .build();
        this.jwkSet = new JWKSet(jwk).toJSONObject();
    }

    @GetMapping("/.well-known/jwks.json")
    @Operation(summary = "Get the JSON Web Key Set", description = "Standard JWKS document (RFC 7517). Public, unauthenticated — every resource server in the system polls this to validate JWTs by their kid.")
    public Map<String, Object> jwks() {
        return jwkSet;
    }
}
