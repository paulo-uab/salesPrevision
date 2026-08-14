package com.uab.core.exception;

/**
 * Thrown by a Resilience4j fallback method when a downstream service call
 * exhausted its retries or the circuit breaker is open — mapped to 503, so
 * callers can distinguish "the request itself is wrong" (400) from "we
 * couldn't reach a dependency right now, try again later" (503).
 */
public class ServiceUnavailableException extends RuntimeException {

    private final Object[] args;

    public ServiceUnavailableException(String messageKey) {
        super(messageKey);
        this.args = null;
    }

    public ServiceUnavailableException(String messageKey, Object... args) {
        super(messageKey);
        this.args = args;
    }

    public Object[] getArgs() {
        return args;
    }
}
