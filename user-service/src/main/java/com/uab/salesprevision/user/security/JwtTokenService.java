package com.uab.salesprevision.user.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.stereotype.Service;

import java.security.interfaces.RSAPrivateKey;
import java.util.Date;
import java.util.List;

@Service
public class JwtTokenService {

    private final RSAPrivateKey privateKey;
    private final String keyId;
    private final long expirationMs;

    public JwtTokenService(JwtProperties props) {
        this.privateKey = RsaKeyUtils.parsePrivateKey(props.getPrivateKey());
        this.keyId = props.getKeyId();
        this.expirationMs = props.getExpirationMs();
    }

    public String generateToken(String username, List<String> roles, Long companyId) {
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(username)
                    .claim("roles", roles)
                    .claim("companyId", companyId)
                    .issueTime(new Date())
                    .expirationTime(new Date(System.currentTimeMillis() + expirationMs))
                    .build();

            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(keyId).build();
            SignedJWT jwt = new SignedJWT(header, claims);
            jwt.sign(new RSASSASigner(privateKey));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("Erro ao gerar token JWT", e);
        }
    }
}
