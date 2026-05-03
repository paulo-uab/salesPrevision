package com.uab.core.exception;

public class ResourceNotFoundException extends RuntimeException {

    private final Object[] args;

    public ResourceNotFoundException(String messageKey) {
        super(messageKey);
        this.args = null;
    }

    public ResourceNotFoundException(String messageKey, Object... args) {
        super(messageKey);
        this.args = args;
    }

    public Object[] getArgs() {
        return args;
    }
}
