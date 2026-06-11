package com.tutornet.tutor_net.exception;

public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }

    public static BusinessException alreadyExists(String message) {
        return new BusinessException(message);
    }

    public static BusinessException conflict(String message) {
        return new BusinessException(message);
    }

    public static BusinessException invalidState(String message) {
        return new BusinessException(message);
    }

    public static BusinessException validationFailed(String message) {
        return new BusinessException(message);
    }

    public static BusinessException forbidden(String message) {
        return new BusinessException(message);
    }
}
