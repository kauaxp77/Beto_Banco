package com.betobanco.shared.exception;

public enum ErrorCode {

    VALIDATION_ERROR(422),
    MALFORMED_REQUEST(400),
    UNAUTHORIZED(401),
    FORBIDDEN(403),
    RESOURCE_NOT_FOUND(404),
    METHOD_NOT_ALLOWED(405),
    NOT_ACCEPTABLE(406),
    CONFLICT(409),
    PAYLOAD_TOO_LARGE(413),
    UNSUPPORTED_MEDIA_TYPE(415),
    RATE_LIMIT_EXCEEDED(429),
    INTERNAL_ERROR(500);

    private final int httpStatus;

    ErrorCode(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    public int httpStatus() {
        return httpStatus;
    }
}
