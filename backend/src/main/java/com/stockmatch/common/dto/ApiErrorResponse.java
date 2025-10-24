package com.stockmatch.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.stockmatch.common.exception.ErrorCode;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {

    private final boolean success = false;
    private int status;
    private String errorCode;
    private String message;
    private Map<String, String> errors; //필드별 에러
    private LocalDateTime timestamp;

    public static ApiErrorResponse of(ErrorCode code, Map<String, String> errors) {
        return ApiErrorResponse.builder()
                .status(code.getStatus().value())
                .errorCode(code.getErrorCode())
                .message(code.getMessage())
                .errors(errors)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static ApiErrorResponse of(ErrorCode code){
        return ApiErrorResponse.builder()
                .status(code.getStatus().value())
                .errorCode(code.getErrorCode())
                .message(code.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
    }

}
