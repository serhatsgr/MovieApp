package com.serhatsgr.exception;

public class BaseException extends RuntimeException {

    private ErrorMessage errorMessage;

    public BaseException() { }

    public BaseException(ErrorMessage errorMessage) {
        super(errorMessage != null ? errorMessage.prepareErrorMessage() : null);
        this.errorMessage = errorMessage;
    }

    public ErrorMessage getErrorMessage() {
        return errorMessage;
    }
}
