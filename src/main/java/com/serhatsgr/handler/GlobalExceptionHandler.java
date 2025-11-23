package com.serhatsgr.handler;

import com.serhatsgr.exception.*;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.AccessDeniedException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final MessageSource messageSource;

    @Autowired
    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    // --- Helper Methods ---
    private String resolveTraceId(HttpServletRequest request) {
        String trace = request != null ? request.getHeader("X-Trace-Id") : null;
        return (trace != null && !trace.isBlank()) ? trace : UUID.randomUUID().toString();
    }

    private String getHostNameSafe() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            logger.warn("Unable to resolve host name: {}", e.getMessage());
            return null;
        }
    }

    private String getLocalizedMessage(String code, Object... args) {
        try {
            return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
        } catch (Exception ex) {
            logger.debug("MessageSource lookup failed for code {}: {}", code, ex.getMessage());
            return code;
        }
    }

    private ApiError buildApiError(HttpStatus status, String message, String code, String path, String traceId, Object details) {
        ApiError apiError = new ApiError();
        apiError.setStatus(status.value());
        apiError.setError(status.getReasonPhrase());
        apiError.setCode(code);
        apiError.setMessage(message);
        apiError.setTimestamp(java.time.Instant.now());
        apiError.setPath(path);
        apiError.setTraceId(traceId);
        apiError.setDetails(details);
        return apiError;
    }

    // --- Handlers ---

    // BaseException
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiError> handleBaseException(BaseException ex, HttpServletRequest request) {
        ErrorMessage em = ex.getErrorMessage();
        MessageType mt = em != null ? em.getMessageType() : null;
        HttpStatus status = (mt != null) ? mt.getHttpStatus() : HttpStatus.BAD_REQUEST;
        String message = (mt != null) ? getLocalizedMessage(mt.getMessageKey()) : "Beklenmeyen bir hata oluştu";
        String code = (mt != null) ? mt.getCode() : "9999";

        logger.warn("BaseException -> code: {}, message: {}, path: {}", code, message, request.getRequestURI());

        Object details = (em != null && em.getOfStatic() != null && !em.getOfStatic().trim().isEmpty()) ? em.getOfStatic() : null;

        ApiError apiError = buildApiError(status, message, code, request.getRequestURI(), resolveTraceId(request), details);
        return ResponseEntity.status(status).body(apiError);
    }

    // Validation: @Valid
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        boolean allFieldsEmpty = ex.getBindingResult().getFieldErrors().stream()
                .allMatch(error -> {
                    Object rejected = error.getRejectedValue();
                    return rejected == null || (rejected instanceof String && ((String) rejected).isBlank());
                });

        if (allFieldsEmpty) {
            ErrorMessage errorMessage = new ErrorMessage(MessageType.BAD_REQUEST, "Film bilgisi boş olamaz");
            Map<String, List<String>> details = Map.of("film", List.of("Film bilgisi boş olamaz"));

            ApiError apiError = buildApiError(
                    HttpStatus.BAD_REQUEST,
                    errorMessage.prepareErrorMessage(),
                    errorMessage.getMessageType().getCode(),
                    request.getRequestURI(),
                    resolveTraceId(request),
                    details
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError);
        }

        Map<String, List<String>> validationErrors = ex.getBindingResult().getFieldErrors()
                .stream()
                .collect(Collectors.groupingBy(
                        fieldError -> fieldError.getField(),
                        Collectors.mapping(fieldError -> fieldError.getDefaultMessage(), Collectors.toList())
                ));

        ErrorMessage errorMessage = new ErrorMessage(MessageType.VALIDATION_ERROR, "Doğrulama hatası.");
        ApiError apiError = buildApiError(
                HttpStatus.BAD_REQUEST,
                errorMessage.prepareErrorMessage(),
                errorMessage.getMessageType().getCode(),
                request.getRequestURI(),
                resolveTraceId(request),
                validationErrors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError);
    }

    // ConstraintViolation
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, List<String>> errors = new HashMap<>();
        for (ConstraintViolation<?> cv : ex.getConstraintViolations()) {
            String path = cv.getPropertyPath().toString();
            errors.computeIfAbsent(path, k -> new ArrayList<>()).add(cv.getMessage());
        }
        MessageType mt = MessageType.VALIDATION_ERROR;
        String message = getLocalizedMessage(mt.getMessageKey());
        ApiError apiError = buildApiError(HttpStatus.BAD_REQUEST, message, mt.getCode(), request.getRequestURI(), resolveTraceId(request), errors);

        logger.info("Constraint violations: {}", errors);
        return ResponseEntity.badRequest().body(apiError);
    }

    // Eksik parametre
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingServletRequestParameter(MissingServletRequestParameterException ex, HttpServletRequest request) {
        String message = String.format("'%s' parametresi eksik.", ex.getParameterName());
        Map<String, Object> details = Map.of(
                "field", ex.getParameterName(),
                "expectedType", ex.getParameterType()
        );
        ApiError apiError = buildApiError(HttpStatus.BAD_REQUEST, message, MessageType.BAD_REQUEST.getCode(), request.getRequestURI(), resolveTraceId(request), details);
        return ResponseEntity.badRequest().body(apiError);
    }

    // PathVariable veya query param tip hatası
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String field = ex.getName();
        String invalidValue = ex.getValue() != null ? ex.getValue().toString() : "null";

        String expectedType;
        if (ex.getRequiredType() != null) {
            Class<?> type = ex.getRequiredType();
            if (type.equals(Long.class) || type.equals(Integer.class)) {
                expectedType = "sayısal bir değer (ör: 123)";
            } else if (type.equals(Boolean.class)) {
                expectedType = "true veya false";
            } else {
                expectedType = type.getSimpleName();
            }
        } else {
            expectedType = "beklenen tür";
        }

        String message = String.format("'%s' parametresi geçersiz. Beklenen: %s. Girilen değer: '%s'.", field, expectedType, invalidValue);

        Map<String, Object> details = Map.of(
                "field", field,
                "expectedType", expectedType,
                "invalidValue", invalidValue
        );

        ApiError apiError = buildApiError(HttpStatus.BAD_REQUEST, message, MessageType.VALIDATION_ERROR.getCode(), request.getRequestURI(), resolveTraceId(request), details);

        logger.warn("Path variable type mismatch: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError);
    }

    // JSON parse hataları (tarih, int/string vb.)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        String fieldName = "";
        String expectedType = "";
        String invalidValue = "unknown";

        Throwable cause = ex.getCause();
        if (cause instanceof InvalidFormatException ife && !ife.getPath().isEmpty()) {
            fieldName = ife.getPath().get(0).getFieldName();
            Class<?> targetType = ife.getTargetType();

            if (targetType.equals(LocalDate.class)) {
                expectedType = "yyyy-MM-dd formatında bir tarih";
            } else if (targetType.equals(Long.class) || targetType.equals(Integer.class)) {
                expectedType = "sayısal bir değer (ör: 123)";
            } else if (targetType.equals(Boolean.class)) {
                expectedType = "true veya false";
            } else if (Enum.class.isAssignableFrom(targetType)) {
                expectedType = "geçerli bir enum değeri";
            } else {
                expectedType = targetType.getSimpleName();
            }

            invalidValue = String.valueOf(ife.getValue());
        }

        String message = fieldName.isEmpty()
                ? "Geçersiz veri formatı."
                : String.format("'%s' alanı geçersiz. Beklenen: %s. Girilen değer: '%s'.", fieldName, expectedType, invalidValue);

        Map<String, Object> details = Map.of(
                "field", fieldName,
                "expectedType", expectedType,
                "invalidValue", invalidValue
        );

        ApiError apiError = buildApiError(HttpStatus.BAD_REQUEST, message, MessageType.VALIDATION_ERROR.getCode(), request.getRequestURI(), resolveTraceId(request), details);

        logger.warn("Invalid JSON format: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError);
    }

    // Diğer exceptionlar (Authentication, AccessDenied, JWT, DataIntegrity, NoHandlerFound vb.)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        MessageType mt = MessageType.FORBIDDEN;
        String message = getLocalizedMessage(mt.getMessageKey());
        ApiError apiError = buildApiError(HttpStatus.FORBIDDEN, message, mt.getCode(), request.getRequestURI(), resolveTraceId(request), null);
        logger.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(apiError);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
        MessageType mt = MessageType.AUTHENTICATION_FAILED;
        String message = getLocalizedMessage(mt.getMessageKey());
        ApiError apiError = buildApiError(HttpStatus.UNAUTHORIZED, message, mt.getCode(), request.getRequestURI(), resolveTraceId(request), null);
        logger.warn("Authentication failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(apiError);
    }

    @ExceptionHandler({JwtException.class, ExpiredJwtException.class})
    public ResponseEntity<ApiError> handleJwtException(Exception ex, HttpServletRequest request) {
        MessageType mt = (ex instanceof ExpiredJwtException) ? MessageType.TOKEN_EXPIRED : MessageType.AUTHENTICATION_FAILED;
        String message = getLocalizedMessage(mt.getMessageKey());
        ApiError apiError = buildApiError(HttpStatus.UNAUTHORIZED, message, mt.getCode(), request.getRequestURI(), resolveTraceId(request), ex.getMessage());
        logger.warn("JWT error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(apiError);
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(org.springframework.dao.DataIntegrityViolationException ex, HttpServletRequest request) {
        MessageType mt = MessageType.DATA_INTEGRITY_VIOLATION;
        String message = getLocalizedMessage(mt.getMessageKey());
        ApiError apiError = buildApiError(HttpStatus.CONFLICT, message, mt.getCode(), request.getRequestURI(), resolveTraceId(request), "Veritabanı kısıtlaması ihlali");
        logger.warn("Data integrity violation: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(apiError);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiError> handleNoHandler(NoHandlerFoundException ex, HttpServletRequest request) {
        MessageType mt = MessageType.NOT_FOUND;
        String message = getLocalizedMessage(mt.getMessageKey());
        ApiError apiError = buildApiError(HttpStatus.NOT_FOUND, message, mt.getCode(), request.getRequestURI(), resolveTraceId(request), null);
        logger.info("No handler found: {}", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiError);
    }
}
