package com.stockmatch.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final ErrorBody error;

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null, null);
    }

    public static ApiResponse<Void> fail(String code, String message) {
        return new ApiResponse<>(false, null, new ErrorBody(code, message));
    }

    @Getter
    @RequiredArgsConstructor
    public static class ErrorBody {
        private final String code;
        private final String message;
    }

}
