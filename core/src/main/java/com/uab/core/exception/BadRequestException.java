package com.uab.core.exception;

public class BadRequestException extends RuntimeException {

    private final Object[] args;

    public BadRequestException(String messageKey) {
        super(messageKey);
        this.args = null;
    }

    public BadRequestException(String messageKey, Object... args) {
        super(messageKey);
        this.args = args;
    }

    public Object[] getArgs() {
        return args;
    }
}
