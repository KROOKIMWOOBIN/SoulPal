package com.soulpal.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 애플리케이션 전역 에러 코드 정의
 * GlobalExceptionHandler 에서 이 코드를 기반으로 응답을 구성합니다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ── 공통 ──────────────────────────────────────────────────────────────────
    INVALID_INPUT(HttpStatus.BAD_REQUEST,         "C001", "입력값이 올바르지 않습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND,      "C002", "요청한 리소스를 찾을 수 없습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED,          "C003", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN,                "C004", "접근 권한이 없습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C005", "서버 오류가 발생했습니다."),

    // ── 인증 ──────────────────────────────────────────────────────────────────
    EMAIL_DUPLICATED(HttpStatus.BAD_REQUEST,       "A001", "이미 사용 중인 이메일입니다."),
    USERNAME_DUPLICATED(HttpStatus.BAD_REQUEST,    "A002", "이미 사용 중인 사용자명입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED,   "A003", "이메일 또는 비밀번호가 올바르지 않습니다."),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED,         "A004", "유효하지 않은 토큰입니다."),
    TOKEN_BLACKLISTED(HttpStatus.UNAUTHORIZED,     "A005", "만료된 토큰입니다. 다시 로그인해주세요."),
    REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "A006", "리프레시 토큰이 유효하지 않습니다."),

    // ── 캐릭터 / 채팅 ─────────────────────────────────────────────────────────
    CHARACTER_NOT_FOUND(HttpStatus.NOT_FOUND,      "CH001", "캐릭터를 찾을 수 없습니다."),
    CHARACTER_FORBIDDEN(HttpStatus.FORBIDDEN,      "CH002", "해당 캐릭터에 대한 권한이 없습니다."),
    MESSAGE_TOO_LONG(HttpStatus.BAD_REQUEST,       "CH003", "메시지가 너무 깁니다. (최대 2000자)"),

    // ── 프로젝트 ──────────────────────────────────────────────────────────────
    PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND,        "P001", "프로젝트를 찾을 수 없습니다."),
    PROJECT_FORBIDDEN(HttpStatus.FORBIDDEN,        "P002", "해당 프로젝트에 대한 권한이 없습니다."),

    // ── Rate Limit ─────────────────────────────────────────────────────────
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "R001", "요청이 너무 많습니다. 잠시 후 다시 시도해주세요."),

    // ── AI / Ollama ───────────────────────────────────────────────────────
    AI_SERVICE_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "AI001", "AI 서비스 응답에 실패했습니다. 잠시 후 다시 시도해주세요.");

    private final HttpStatus httpStatus;
    private final String     code;
    private final String     message;
}
