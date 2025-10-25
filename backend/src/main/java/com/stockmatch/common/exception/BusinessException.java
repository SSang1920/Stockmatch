package com.stockmatch.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    @Override
    public String getMessage() {
        return errorCode.message();
    }
}
