package com.tutornet.tutor_net.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── 404 ─────────────────────────────────────────────────────────────────
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(HttpStatus.NOT_FOUND.value(), ex.getMessage()));
    }

    // ── 422 Nghiệp vụ ───────────────────────────────────────────────────────
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErrorResponse.of(422, ex.getMessage()));
    }

    // ── 400 Validation ──────────────────────────────────────────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            errors.put(field, error.getDefaultMessage());
        });
        ErrorResponse body = ErrorResponse.of(400, "Dữ liệu không hợp lệ");
        body.setValidationErrors(errors);
        return ResponseEntity.badRequest().body(body);
    }

    // ── 500 ─────────────────────────────────────────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex)  {

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(500, "Lỗi hệ thống: " + ex.getMessage()));
    }

    // ── Inner DTO ────────────────────────────────────────────────────────────
    public static class ErrorResponse {
        private int status;
        private String message;
        private Instant timestamp = Instant.now();
        private Map<String, String> validationErrors;

        public static ErrorResponse of(int status, String message) {
            ErrorResponse r = new ErrorResponse();
            r.status = status;
            r.message = message;
            return r;
        }

        public int getStatus() { return status; }
        public String getMessage() { return message; }
        public Instant getTimestamp() { return timestamp; }
        public Map<String, String> getValidationErrors() { return validationErrors; }
        public void setValidationErrors(Map<String, String> v) { this.validationErrors = v; }
    }
}
