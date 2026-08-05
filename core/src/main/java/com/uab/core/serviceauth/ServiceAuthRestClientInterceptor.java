package com.uab.core.serviceauth;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

public class ServiceAuthRestClientInterceptor implements ClientHttpRequestInterceptor {

    private final ServiceTokenProvider tokenProvider;

    public ServiceAuthRestClientInterceptor(ServiceTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request,
                                        byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        request.getHeaders().setBearerAuth(tokenProvider.getToken());
        return execution.execute(request, body);
    }
}
