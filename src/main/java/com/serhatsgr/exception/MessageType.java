package com.serhatsgr.exception;

import org.springframework.http.HttpStatus;

public enum MessageType {

    GENERAL_EXCEPTION("9999", "error.general_exception", HttpStatus.INTERNAL_SERVER_ERROR),

    // 1xxx - client/request
    BAD_REQUEST("1000", "error.bad_request", HttpStatus.BAD_REQUEST),
    NOT_FOUND("1001", "error.not_found", HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED("1002", "error.method_not_allowed", HttpStatus.METHOD_NOT_ALLOWED),
    UNSUPPORTED_MEDIA_TYPE("1004", "error.unsupported_media_type", HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    VALIDATION_ERROR("1100", "error.validation", HttpStatus.BAD_REQUEST),

    // 2xxx - auth
    UNAUTHORIZED("2000", "error.unauthorized", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("2001", "error.forbidden", HttpStatus.FORBIDDEN),
    AUTHENTICATION_FAILED("2002", "error.authentication_failed", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED("2003", "error.token_expired", HttpStatus.UNAUTHORIZED),
    RESOURCE_NOT_FOUND("1006", "error.resource_not_found", HttpStatus.NOT_FOUND),

    // 3xxx - business
    BUSINESS_RULE_VIOLATION("3000", "error.business_rule", HttpStatus.CONFLICT),
    DUPLICATE_RESOURCE("3001", "error.duplicate_resource", HttpStatus.CONFLICT),
    CONFLICT("3002", "error.conflict", HttpStatus.CONFLICT),

    // 4xxx - data
    DATA_INTEGRITY_VIOLATION("4000", "error.data_integrity", HttpStatus.CONFLICT),
    UNIQUE_CONSTRAINT("4002", "error.unique_constraint", HttpStatus.CONFLICT),
    OPTIMISTIC_LOCK("4004", "error.optimistic_lock", HttpStatus.CONFLICT),

    // 5xxx - external
    SERVICE_UNAVAILABLE("5000", "error.service_unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    TIMEOUT("5001", "error.timeout", HttpStatus.GATEWAY_TIMEOUT),
    DOWNSTREAM_ERROR("5002", "error.downstream", HttpStatus.BAD_GATEWAY),

    // 9xxx - server
    INTERNAL_ERROR("9000", "error.internal", HttpStatus.INTERNAL_SERVER_ERROR),
    CONFIGURATION_ERROR("9001", "error.configuration", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String messageKey;
    private final HttpStatus httpStatus;

    MessageType(String code, String messageKey, HttpStatus httpStatus) {
        this.code = code;
        this.messageKey = messageKey;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}