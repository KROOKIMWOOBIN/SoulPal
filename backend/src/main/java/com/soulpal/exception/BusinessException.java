package com.soulpal.exception;

import lombok.Getter;

/**
 * 비즈니스 로직 오류 기본 클래스.
 * ErrorCode 를 기반으로 상태코드와 메시지를 관리합니다.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String detail) {
        super(detail);
        this.errorCode = errorCode;
    }
}
