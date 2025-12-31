package com.serhatsgr.handler;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    private String getLocalizedMessage(String code, Object... args) {
        try {
            return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
        } catch (Exception ex) {
            return code;
        }
    }

    private ApiError buildApiError(HttpStatus status, String message, String code, String path, String traceId, Object details) {
        ApiError apiError = new ApiError();
        apiError.setStatus(status.value());
        apiError.setError(status.getReasonPhrase());
        apiError.setCode(code);
        apiError.setMessage(message);
        apiError.setTimestamp(LocalDateTime.now());
        apiError.setPath(path);
        apiError.setTraceId(traceId);
        apiError.setDetails(details);
        return apiError;
    }

    // --- Handlers ---

    // 1. JSON Format Hataları (Tarih, Sayı vb.) - SADELEŞTİRİLDİ
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        // Varsayılan mesaj
        String message = "Girdiğiniz verilerde format hatası var.";

        // Jackson hatasını kontrol et
        if (ex.getCause() instanceof InvalidFormatException) {
            InvalidFormatException ife = (InvalidFormatException) ex.getCause();

            // Tarih hatasıysa (LocalDate)
            if (ife.getTargetType().equals(LocalDate.class)) {
                message = "Geçersiz bir tarih girdiniz. Lütfen (Yıl-Ay-Gün) formatında giriniz.";
            }
            // Sayı hatasıysa (Long, Integer, int, long vb.)
            else if (Number.class.isAssignableFrom(ife.getTargetType()) ||
                    ife.getTargetType().equals(int.class) ||
                    ife.getTargetType().equals(long.class)) {
                message = "Bu alana sadece sayısal değer girebilirsiniz.";
            }
            // Enum hatasıysa (Seçenekler)
            else if (Enum.class.isAssignableFrom(ife.getTargetType())) {
                message = "Seçtiğiniz değer geçerli seçenekler arasında değil.";
            }
        }

        // Loglara teknik hatayı basıyoruz, kullanıcıya sade mesajı dönüyoruz
        logger.warn("JSON Parse Error: {}", ex.getMessage());

        ApiError apiError = buildApiError(
                HttpStatus.BAD_REQUEST,
                message,
                MessageType.VALIDATION_ERROR.getCode(),
                request.getRequestURI(),
                resolveTraceId(request),
                null
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError);
    }

    // 2. BaseException (Özel İş Kuralları)
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiError> handleBaseException(BaseException ex, HttpServletRequest request) {
        ErrorMessage em = ex.getErrorMessage();
        MessageType mt = em != null ? em.getMessageType() : MessageType.GENERAL_EXCEPTION;
        HttpStatus status = (mt != null) ? mt.getHttpStatus() : HttpStatus.BAD_REQUEST;
        String code = (mt != null) ? mt.getCode() : "9999";

        String baseMessage = (mt != null) ? getLocalizedMessage(mt.getMessageKey()) : "Hata oluştu";
        // Özel mesaj varsa onu kullan, yoksa genel mesajı kullan
        String finalMessage = (em != null && em.getOfStatic() != null) ? em.getOfStatic() : baseMessage;

        ApiError apiError = buildApiError(status, finalMessage, code, request.getRequestURI(), resolveTraceId(request), null);
        return ResponseEntity.status(status).body(apiError);
    }

    // 3. Validasyon Hataları (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {

        String errorMessage = "Validasyon hatası";
        if (!ex.getBindingResult().getFieldErrors().isEmpty()) {
            errorMessage = ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        }

        // Detayları yine de structured olarak ekleyelim (Frontend isterse kullanır)
        Map<String, List<String>> validationDetails = ex.getBindingResult().getFieldErrors()
                .stream()
                .collect(Collectors.groupingBy(
                        fieldError -> fieldError.getField(),
                        Collectors.mapping(fieldError -> fieldError.getDefaultMessage(), Collectors.toList())
                ));

        ApiError apiError = buildApiError(
                HttpStatus.BAD_REQUEST,
                errorMessage,
                MessageType.VALIDATION_ERROR.getCode(),
                request.getRequestURI(),
                resolveTraceId(request),
                validationDetails
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError);
    }

    // 4. Constraint Violation
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        String message = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .findFirst()
                .orElse("Validasyon hatası");

        ApiError apiError = buildApiError(HttpStatus.BAD_REQUEST, message, MessageType.VALIDATION_ERROR.getCode(), request.getRequestURI(), resolveTraceId(request), null);
        return ResponseEntity.badRequest().body(apiError);
    }

    // 5. Parametre Hataları
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String message = "Girdiğiniz parametre formatı hatalı.";
        ApiError apiError = buildApiError(HttpStatus.BAD_REQUEST, message, MessageType.VALIDATION_ERROR.getCode(), request.getRequestURI(), resolveTraceId(request), null);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError);
    }

    // 6. Security Hataları
    @ExceptionHandler({AuthenticationException.class, JwtException.class, ExpiredJwtException.class, AccessDeniedException.class})
    public ResponseEntity<ApiError> handleSecurityExceptions(Exception ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        String message = "Oturum hatası";
        String code = MessageType.UNAUTHORIZED.getCode();

        if (ex instanceof AccessDeniedException) {
            status = HttpStatus.FORBIDDEN;
            message = "Bu işlem için yetkiniz yok.";
            code = MessageType.FORBIDDEN.getCode();
        } else if (ex instanceof ExpiredJwtException) {
            message = "Oturum süreniz doldu.";
            code = MessageType.TOKEN_EXPIRED.getCode();
        } else if (ex instanceof DisabledException) {
            message = "Hesabınız erişime kapatılmıştır";
            code = "ACCOUNT_DISABLED";
        } else {
            message = "Giriş yapmanız gerekiyor.";
        }

        ApiError apiError = buildApiError(status, message, code, request.getRequestURI(), resolveTraceId(request), null);
        return ResponseEntity.status(status).body(apiError);
    }

    // 7. Genel Hata
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleAll(Exception ex, HttpServletRequest request) {
        logger.error("Unexpected Error: ", ex);
        ApiError apiError = buildApiError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Sunucuda geçici bir sorun oluştu.",
                MessageType.INTERNAL_ERROR.getCode(),
                request.getRequestURI(),
                resolveTraceId(request),
                null
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiError);
    }
}