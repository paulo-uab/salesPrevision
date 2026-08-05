package com.uab.salesprevision.user.security;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

final class RsaKeyUtils {

    private RsaKeyUtils() {
    }

    static RSAPrivateKey parsePrivateKey(String base64Der) {
        try {
            byte[] bytes = Base64.getDecoder().decode(base64Der);
            KeyFactory factory = KeyFactory.getInstance("RSA");
            return (RSAPrivateKey) factory.generatePrivate(new PKCS8EncodedKeySpec(bytes));
        } catch (Exception e) {
            throw new IllegalStateException("Invalid app.jwt.private-key", e);
        }
    }

    static RSAPublicKey parsePublicKey(String base64Der) {
        try {
            byte[] bytes = Base64.getDecoder().decode(base64Der);
            KeyFactory factory = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) factory.generatePublic(new X509EncodedKeySpec(bytes));
        } catch (Exception e) {
            throw new IllegalStateException("Invalid app.jwt.public-key", e);
        }
    }
}
