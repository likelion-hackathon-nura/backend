package org.example.nura.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "요청 값이 올바르지 않습니다."),
    INVALID_REQUEST_BODY(HttpStatus.BAD_REQUEST, "C002", "요청 본문을 읽을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C003", "지원하지 않는 HTTP 메서드입니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "C004", "요청한 리소스를 찾을 수 없습니다."),
    ENDPOINT_NOT_FOUND(HttpStatus.NOT_FOUND, "C005", "존재하지 않는 엔드포인트입니다."),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "C006", "요청 값의 타입이 올바르지 않습니다."),
    MISSING_REQUEST_PARAMETER(HttpStatus.BAD_REQUEST, "C007", "필수 요청 파라미터가 누락되었습니다."),
    MISSING_REQUEST_HEADER(HttpStatus.BAD_REQUEST, "C008", "필수 요청 헤더가 누락되었습니다."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "C009", "지원하지 않는 Content-Type입니다."),
    MULTIPART_REQUEST_ERROR(HttpStatus.BAD_REQUEST, "C010", "파일 업로드 요청을 처리할 수 없습니다."),
    FILE_SIZE_EXCEEDED(HttpStatus.PAYLOAD_TOO_LARGE, "C011", "업로드 가능한 파일 크기를 초과했습니다."),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "C012", "이미 존재하는 리소스입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C500", "서버 내부 오류가 발생했습니다."),

    // Auth
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A001", "인증이 필요합니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A002", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "A003", "만료된 토큰입니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "A004", "접근 권한이 없습니다."),
    TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "A005", "인증 토큰이 존재하지 않습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
