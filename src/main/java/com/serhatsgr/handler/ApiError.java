package com.serhatsgr.handler;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {

    private int status;
    private String error;
    private String code;
    private String message;
    private String path;
    private String traceId;
    private Object details;
    private LocalDateTime timestamp;


    public ApiError() {
        this.timestamp = LocalDateTime.now();
    }

    public ApiError(int status, String error, String code, String message,
                    String path, String traceId, Object details,
                    LocalDateTime timestamp) {
        this.status = status;
        this.error = error;
        this.code = code;
        this.message = message;
        this.path = path;
        this.traceId = traceId;
        this.details = details;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
    }

    //Builder
    public static class Builder {
        private int status;
        private String error;
        private String code;
        private String message;
        private String path;
        private String traceId;
        private Object details;
        private LocalDateTime timestamp;

        public Builder status(int status) {
            this.status = status;
            return this;
        }

        public Builder error(String error) {
            this.error = error;
            return this;
        }

        public Builder code(String code) {
            this.code = code;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder traceId(String traceId) {
            this.traceId = traceId;
            return this;
        }

        public Builder details(Object details) {
            this.details = details;
            return this;
        }

        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public ApiError build() {
            return new ApiError(status, error, code, message, path, traceId, details, timestamp);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    // --- Getter / Setter ---

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }

    public Object getDetails() { return details; }
    public void setDetails(Object details) { this.details = details; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
