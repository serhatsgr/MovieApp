package com.serhatsgr.handler;

import java.time.Instant;
import java.util.Map;

public class ApiError {

    private int status;
    private String error;        // HttpStatus reason phrase
    private String code;         // senin MessageType.code
    private String message;      // kullanıcıya gösterilecek mesaj (localized)
    private Instant timestamp;
    private String path;
    private String traceId;
    private Object details;      // validation details veya exception objesi (isteğe bağlı)

    public ApiError() {
        this.timestamp = Instant.now();
    }

    // getters & setters

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public Object getDetails() {
        return details;
    }

    public void setDetails(Object details) {
        this.details = details;
    }
}
