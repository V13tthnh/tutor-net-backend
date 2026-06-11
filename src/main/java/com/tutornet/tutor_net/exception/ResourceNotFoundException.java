package com.tutornet.tutor_net.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }

    // Factory methods tiện dụng
    public static ResourceNotFoundException of(String resource, Long id) {
        return new ResourceNotFoundException(resource + " không tồn tại với id: " + id);
    }

    public static ResourceNotFoundException of(String resource, String field, Object value) {
        return new ResourceNotFoundException(resource + " không tồn tại với " + field + ": " + value);
    }
}
