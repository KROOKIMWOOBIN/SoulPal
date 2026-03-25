package com.soulpal.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 전역 예외 핸들러.
 * 모든 예외를 ErrorCode 기반 표준 응답으로 변환합니다.
 *
 * 응답 형식:
 * {
 *   "code":      "A001",
 *   "message":   "이미 사용 중인 이메일입니다.",
 *   "timestamp": "2026-03-24T12:00:00Z"
 * }
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── ErrorCode 기반 비즈니스 예외 ───────────────────────────────────────────
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusiness(BusinessException e) {
        log.warn("[BIZ] {} {}", e.getErrorCode().getCode(), e.getMessage());
        return build(e.getErrorCode(), e.getMessage());
    }

    // ── 레거시: IllegalArgumentException → INVALID_INPUT ────────────────────
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArg(IllegalArgumentException e) {
        log.warn("[BIZ] C001 {}", e.getMessage());
        return build(ErrorCode.INVALID_INPUT, e.getMessage());
    }

    // ── @Valid 검증 실패 ──────────────────────────────────────────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return build(ErrorCode.INVALID_INPUT, detail);
    }

    // ── 리소스 없음 ────────────────────────────────────────────────────────────
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException e) {
        return build(ErrorCode.RESOURCE_NOT_FOUND, e.getMessage());
    }

    // ── Rate Limit 초과 ───────────────────────────────────────────────────────
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimit(RateLimitExceededException e) {
        return build(ErrorCode.RATE_LIMIT_EXCEEDED, e.getMessage());
    }

    // ── 미처리 예외 (스택트레이스 클라이언트 미노출) ──────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception e) {
        log.error("[UNHANDLED] {} : {}", e.getClass().getSimpleName(), e.getMessage(), e);
        return build(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.getMessage());
    }

    // ── 응답 빌더 ─────────────────────────────────────────────────────────────
    private ResponseEntity<Map<String, Object>> build(ErrorCode code, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code",      code.getCode());
        body.put("message",   message);
        body.put("timestamp", Instant.now().toString());
        return ResponseEntity.status(code.getHttpStatus()).body(body);
    }
}
