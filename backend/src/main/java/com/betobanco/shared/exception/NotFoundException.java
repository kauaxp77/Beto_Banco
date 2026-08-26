package com.betobanco.shared.exception;

public class NotFoundException extends BusinessException {

    public NotFoundException(String message) {
        super(ErrorCode.RESOURCE_NOT_FOUND, message);
    }
}
