package org.example.nura.global.common;

import org.example.nura.global.error.ErrorCode;

public record ApiResponse<T>(
        boolean success,
        String code,
        String message,
        T data
) {

    private static final String SUCCESS_CODE = "OK";
    private static final String SUCCESS_MESSAGE = "요청이 성공했습니다.";

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, SUCCESS_CODE, SUCCESS_MESSAGE, data);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, SUCCESS_CODE, SUCCESS_MESSAGE, null);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, SUCCESS_CODE, message, data);
    }

    public static ApiResponse<Void> error(ErrorCode errorCode) {
        return new ApiResponse<>(
                false,
                errorCode.getCode(),
                errorCode.getMessage(),
                null
        );
    }

    public static ApiResponse<Void> error(
            ErrorCode errorCode,
            String message
    ) {
        return new ApiResponse<>(
                false,
                errorCode.getCode(),
                message,
                null
        );
    }
}
