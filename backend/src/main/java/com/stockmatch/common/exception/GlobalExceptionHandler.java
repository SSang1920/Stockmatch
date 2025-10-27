package com.stockmatch.common.exception;

import com.stockmatch.common.api.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 비즈니스 예외: 도메인/서비스에서 의도적으로 던진 예외
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
        var ec = e.getErrorCode();
        log.warn("[BusinessException] code={}, message={}", ec.code(), ec.message());
        return ResponseEntity
                .status(ec.status())
                .body(ApiResponse.fail(ec.code(), ec.message()));
    }

    // Validation 바인딩/검증 오류
    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            BindException.class,
            ConstraintViolationException.class,
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleValidation(Exception e) {
        var ec = ErrorCode.INVALID_INPUT;
        log.warn("[ValidationException] type={}, message={}", e.getClass().getSimpleName(), e.getMessage());
        return ResponseEntity
                .status(ec.status())
                .body(ApiResponse.fail(ec.code(), ec.message()));
    }

    // HTTP 스펙 관련
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
        var ec = ErrorCode.METHOD_NOT_ALLOWED;
        log.warn("[MethodNotAllowedException] {}", e.getMessage());
        return ResponseEntity
                .status(ec.status())
                .body(ApiResponse.fail(ec.code(), ec.message()));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMediaType(HttpMediaTypeNotSupportedException e) {
        var ec = ErrorCode.MEDIA_TYPE_NOT_SUPPORTED;
        log.warn("[MediaTypeNotSupportedException] {}", e.getMessage());
        return ResponseEntity
                .status(ec.status())
                .body(ApiResponse.fail(ec.code(), ec.message()));
    }

    // 인증과정에서 발생한 예외 처리
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(AuthenticationException e){

        // 토큰 만료, 형식 오류등의 예외 처리
        if (e instanceof JwtAuthenticationException jae) {
            var ec = jae.getErrorCode();
            log.warn("[JwtAuthenticationException] code={}, message={}", ec.code(), ec.message());
            return ResponseEntity
                    .status(ec.status())
                    .body(ApiResponse.fail(ec.code(), ec.message()));
        }

        var ec = ErrorCode.UNAUTHORIZED;
        log.warn("[AuthenticationException] {}", e.getMessage());
        return ResponseEntity
                .status(ec.status())
                .body(ApiResponse.fail(ec.code(), ec.message()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException e) {
        var ec = ErrorCode.ACCESS_DENIED;
        log.warn("[AccessDeniedException] {}", e.getMessage());
        return ResponseEntity
                .status(ec.status())
                .body(ApiResponse.fail(ec.code(), ec.message()));
    }

    // 그 외 시스템 예외처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleEtc(Exception e) {
        var ec = ErrorCode.INTERNAL_ERROR;
        log.error("[Exception] {}", e.getMessage(), e);
        return ResponseEntity
                .status(ec.status())
                .body(ApiResponse.fail(ec.code(), ec.message()));
    }
}
