package com.uab.core.tokenrelay;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;

/**
 * Forwards the inbound request's Authorization header onto outbound RestClient
 * calls. Used instead of minting a fresh service-account token so that
 * downstream services see the original caller's identity (and, critically,
 * their companyId claim) rather than a generic machine identity that has no
 * company context of its own.
 * <p>
 * Only meaningful inside a real inbound HTTP request (a controller handling a
 * user's call). Background/scheduled work has no request to relay a token
 * from and needs a different mechanism (a dedicated service account).
 */
public class TokenRelayRestClientInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request,
                                        byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest inbound = attributes.getRequest();
            String authorization = inbound.getHeader(HttpHeaders.AUTHORIZATION);
            if (authorization != null) {
                request.getHeaders().set(HttpHeaders.AUTHORIZATION, authorization);
            }
        }
        return execution.execute(request, body);
    }
}
