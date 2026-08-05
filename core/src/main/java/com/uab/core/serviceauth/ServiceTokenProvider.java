package com.uab.core.serviceauth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

import java.time.Instant;

/**
 * Logs in against gateway-service's /auth/login with a machine account and caches
 * the resulting JWT until shortly before it expires. Used to authenticate outbound
 * server-to-server calls that are routed through the gateway.
 * <p>
 * Deliberately builds its own plain {@link RestClient} instead of taking an
 * injected {@code RestClient.Builder} — that builder is customized by
 * {@link ServiceAuthAutoConfiguration} to attach this very token, which would
 * make the login call depend on a token it hasn't fetched yet.
 */
public class ServiceTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(ServiceTokenProvider.class);
    private static final long EXPIRY_SAFETY_MARGIN_MS = 30_000;

    private final RestClient restClient;
    private final String username;
    private final String password;

    private volatile String cachedToken;
    private volatile Instant expiresAt = Instant.MIN;

    public ServiceTokenProvider(String gatewayUrl, String username, String password) {
        this.restClient = RestClient.builder().baseUrl(gatewayUrl).build();
        this.username = username;
        this.password = password;
    }

    public synchronized String getToken() {
        if (cachedToken == null || Instant.now().isAfter(expiresAt)) {
            refresh();
        }
        return cachedToken;
    }

    private void refresh() {
        log.debug("Fetching new service token from gateway for user '{}'", username);
        LoginResponse response = restClient.post()
                .uri("/auth/login")
                .body(new LoginRequest(username, password))
                .retrieve()
                .body(LoginResponse.class);
        if (response == null || response.token() == null) {
            throw new IllegalStateException("Gateway did not return a token for service login");
        }
        cachedToken = response.token();
        expiresAt = Instant.now().plusMillis(response.expiresInMs()).minusMillis(EXPIRY_SAFETY_MARGIN_MS);
        log.debug("Service token refreshed, valid until {}", expiresAt);
    }
}
