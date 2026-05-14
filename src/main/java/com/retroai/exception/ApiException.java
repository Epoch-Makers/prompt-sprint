package com.retroai.exception;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final String errorCode;

    public ApiException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() { return status; }
    public String getErrorCode() { return errorCode; }

    public static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }

    public static ApiException unauthorized(String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", message);
    }

    public static ApiException forbidden(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", message);
    }

    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }

    public static ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, "CONFLICT", message);
    }

    public static ApiException timeout(String message) {
        return new ApiException(HttpStatus.GATEWAY_TIMEOUT, "GATEWAY_TIMEOUT", message);
    }

    public static ApiException serviceUnavailable(String errorCode, String message) {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, errorCode, message);
    }

    /** 410 Gone — used when a closed retro is accessed by a guest. */
    public static ApiException gone(String message) {
        return new ApiException(HttpStatus.GONE, "RETRO_CLOSED", message);
    }

    /** 423 Locked — used when a card/vote operation is attempted in the wrong phase. */
    public static ApiException phaseLocked(String message) {
        return new ApiException(HttpStatus.LOCKED, "PHASE_LOCKED", message);
    }

    /** 502 Bad Gateway — Atlassian / external upstream failures. */
    public static ApiException badGateway(String message) {
        return new ApiException(HttpStatus.BAD_GATEWAY, "BAD_GATEWAY", message);
    }
}
