package com.stockmatch.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통
    INTERNAL_ERROR("C000", "알 수 없는 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_INPUT("C001", "요청 값이 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
    METHOD_NOT_ALLOWED("C002", "허용되지 않은 메서드입니다.", HttpStatus.METHOD_NOT_ALLOWED),
    MEDIA_TYPE_NOT_SUPPORTED("C003", "지원하지 않는 콘텐츠 타입입니다.", HttpStatus.UNSUPPORTED_MEDIA_TYPE),

    // 인증/인가
    UNAUTHORIZED("A001", "인증이 필요합니다.", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED("A002", "해당 기능에 대한 권한이 없습니다.", HttpStatus.FORBIDDEN),
    TOKEN_EXPIRED("A003", "토큰이 만료되었습니다.", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID("A004", "유효하지 않은 토큰입니다.", HttpStatus.UNAUTHORIZED),

    // 유저
    USER_NOT_FOUND("U001", "사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    USER_ALREADY_EXISTS("U002", "이미 존재하는 사용자입니다.", HttpStatus.CONFLICT),


    // 포트폴리오
    PORTFOLIO_NOT_FOUND("P001", "포트폴리오를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    PORTFOLIO_ALREADY_EXISTS("P002", "이미 존재하는 포트폴리오입니다.", HttpStatus.CONFLICT),

    // 보유 종목
    HOLDING_NOT_FOUND("H001", "보유 종목을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    HOLDING_ALREADY_EXISTS("H002", "이미 존재하는 보유 종목입니다.", HttpStatus.CONFLICT),


    // 종목(Security)
    SECURITY_NOT_FOUND("S001", "종목을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    SECURITY_ALREADY_EXISTS("S002", "이미 존재하는 종목입니다.", HttpStatus.CONFLICT),

    // 외부 API
    RATE_LIMITED("M001", "외부 API 호출 제한에 걸렸습니다.", HttpStatus.TOO_MANY_REQUESTS);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    // 헬퍼 메서드
    public String code() { return code; }
    public String message() { return message; }
    public HttpStatus status() { return httpStatus; }
}
