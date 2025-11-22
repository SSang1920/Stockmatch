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
    OAUTH_TOKEN_EXCHANGE_FAILED("A005", "토큰 교환에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    OAUTH_USERINFO_FAILED("A006", "사용자 정보 조회에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    OAUTH_UNLINK_FAILED("A007", "소셜 서비스와의 연동 해제에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    REFRESH_TOKEN_NOT_FOUND("A008", "Refresh Token이 존재하지 않습니다. 다시 로그인해주세요.", HttpStatus.BAD_REQUEST),

    // 유저
    USER_NOT_FOUND("U001", "사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    USER_ALREADY_EXISTS("U002", "이미 존재하는 사용자입니다.", HttpStatus.CONFLICT),

    // 포트폴리오
    PORTFOLIO_NOT_FOUND("P001", "포트폴리오를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    PORTFOLIO_ALREADY_EXISTS("P002", "이미 존재하는 포트폴리오입니다.", HttpStatus.CONFLICT),

    // 보유 종목
    HOLDING_NOT_FOUND("H001", "보유 종목을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    HOLDING_ALREADY_EXISTS("H002", "이미 존재하는 보유 종목입니다.", HttpStatus.CONFLICT),
    HOLDING_NOT_IN_PORTFOLIO("H003", "포트폴리오에 이 보유종목이 없습니다.", HttpStatus.FORBIDDEN),
    INSUFFICIENT_HOLDING_QUANTITY("H004", "보유 수량이 부족합니다.", HttpStatus.BAD_REQUEST),

    // 환율(ExchangeRate
    UNSUPPORTED_CURRENCY_CONVERSION("E001", "지원하지 않는 통화 변환입니다.", HttpStatus.BAD_REQUEST ),
    EXCHANGE_RATE_NOT_FOUND("E002", "해당 날짜의 환율 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    // 종목(Security)
    SECURITY_NOT_FOUND("S001", "종목을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    SECURITY_ALREADY_EXISTS("S002", "이미 존재하는 종목입니다.", HttpStatus.CONFLICT),
    DUPLICATE_TICKER("S003", "중복된 종목 코드입니다.", HttpStatus.CONFLICT),

    // 외부 API
    EXTERNAL_API_ERROR("E001", "외부 API 호출 중 오류가 발생했습니다.", HttpStatus.BAD_GATEWAY),
    UNSUPPORTED_REGION("E002", "지원하지 않는 지역입니다.", HttpStatus.BAD_REQUEST),
    UPSTREAM_DATA_EMPTY("E003", "외부 API 응답이 비어 있습니다.", HttpStatus.BAD_GATEWAY),
    INVALID_API_KEY("E004", "올바르지 않은 API KEY입니다. (AlphaVantage)", HttpStatus.BAD_REQUEST),
    EXTERNAL_API_DATA_NOT_FOUND("E005", "받아온 데이터의 값이 올바른 값이 아닙니다.", HttpStatus.BAD_REQUEST),
    API_KEY_NOT_REGISTERED("E006", "API KEY가 존재하지 않습니다. (AlphaVantage)", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    // 헬퍼 메서드
    public String code() { return code; }
    public String message() { return message; }
    public HttpStatus status() { return httpStatus; }
}
