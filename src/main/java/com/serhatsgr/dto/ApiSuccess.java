package com.serhatsgr.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiSuccess<T> {

    private final int status;
    private final String message;
    private final T data;
    private final String traceId;
    private final String timestamp;

    /**
     * Basit success factory metodu
     */
    public static <T> ApiSuccess<T> of(String message, T data) {
        return ApiSuccess.<T>builder()
                .status(200)
                .message(message)
                .data(data)
                .traceId(UUID.randomUUID().toString())
                .timestamp(ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                .build();
    }

    /**
     * Ek kontrol/override gereken durumlar için (örnek: farklı status kodu)
     */
    public static <T> ApiSuccess<T> of(int status, String message, T data) {
        return ApiSuccess.<T>builder()
                .status(status)
                .message(message)
                .data(data)
                .traceId(UUID.randomUUID().toString())
                .timestamp(ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                .build();
    }
}
