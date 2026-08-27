package com.betobanco.shared.exception;

import java.util.List;

public record ErrorPayload(
        String code,
        String message,
        int status,
        String path,
        String traceId,
        String timestamp,
        List<FieldErrorItem> fieldErrors) {
}
