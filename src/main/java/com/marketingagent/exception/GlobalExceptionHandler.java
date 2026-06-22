package com.marketingagent.exception;

import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ErrorResponse> handleApplicationException(
            ApplicationException exception,
            HttpServletRequest request
    ) {
        return buildResponse(exception.getStatus(), exception.getErrorCode(), exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        Map<String, String> details = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            details.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ErrorCode.VALIDATION_FAILED,
                "Request validation failed",
                request,
                details
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        Map<String, String> details = new LinkedHashMap<>();
        exception.getConstraintViolations().forEach(violation ->
                details.put(violation.getPropertyPath().toString(), violation.getMessage())
        );
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ErrorCode.VALIDATION_FAILED,
                "Constraint validation failed",
                request,
                details
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.CONFLICT,
                ErrorCode.CONFLICT,
                "Request conflicts with existing data",
                request,
                Map.of()
        );
    }

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(
            org.springframework.web.server.ResponseStatusException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.valueOf(exception.getStatusCode().value()),
                ErrorCode.VALIDATION_FAILED,
                exception.getReason() != null ? exception.getReason() : exception.getMessage(),
                request,
                Map.of()
        );
    }

    @ExceptionHandler(org.springframework.transaction.TransactionSystemException.class)
    public ResponseEntity<ErrorResponse> handleTransactionSystemException(
            org.springframework.transaction.TransactionSystemException exception,
            HttpServletRequest request
    ) {
        Throwable cause = exception.getRootCause();
        if (cause instanceof org.springframework.dao.DataIntegrityViolationException || cause instanceof jakarta.validation.ConstraintViolationException) {
            return buildResponse(
                    HttpStatus.CONFLICT,
                    ErrorCode.CONFLICT,
                    "Request conflicts with existing data (Constraint Violation)",
                    request,
                    Map.of()
            );
        }
        return handleUnexpected(exception, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception, HttpServletRequest request) {
        org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class).error("Unexpected server error", exception);
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCode.INTERNAL_ERROR,
                "Unexpected server error",
                request,
                Map.of()
        );
    }

    private ResponseEntity<ErrorResponse> buildResponse(
            HttpStatus status,
            ErrorCode code,
            String message,
            HttpServletRequest request,
            Map<String, String> details
    ) {
        ErrorResponse response = new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                code,
                message,
                request.getRequestURI(),
                details
        );
        return ResponseEntity.status(status).body(response);
    }
}
