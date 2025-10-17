package com.renaissance.app.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Align response structure with TaskController
    private Map<String, Object> buildErrorResponse(HttpStatus status, String message, Map<String, String> errors) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("timestamp", LocalDateTime.now());
        response.put("status", status.value());
        if (errors != null && !errors.isEmpty()) {
            response.put("errors", errors);
        }
        return response;
    }

    private Map<String, Object> buildErrorResponse(HttpStatus status, String message) {
        return buildErrorResponse(status, message, null);
    }

    @ExceptionHandler(EmailAlreadyExistException.class)
    public ResponseEntity<Map<String, Object>> handleEmailAlreadyExistException(EmailAlreadyExistException ex) {
        logger.warn("Email already exists: {}", ex.getMessage());
        return new ResponseEntity<>(buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage()), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentialsException(BadCredentialsException ex) {
        logger.warn("Bad credentials: {}", ex.getMessage());
        return new ResponseEntity<>(buildErrorResponse(HttpStatus.UNAUTHORIZED, "Invalid email or password"), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequestException(BadRequestException ex) {
        logger.warn("Bad request: {}", ex.getMessage());
        return new ResponseEntity<>(buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolationException(ConstraintViolationException ex) {
        Map<String, String> errors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        ConstraintViolation::getMessage,
                        (existing, replacement) -> existing)); // Merge duplicate keys
        logger.warn("Constraint violation: {}", errors);
        return new ResponseEntity<>(buildErrorResponse(HttpStatus.BAD_REQUEST, "Validation failed", errors), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingServletRequestParameterException(MissingServletRequestParameterException ex) {
        logger.warn("Missing request parameter: {}", ex.getMessage());
        return new ResponseEntity<>(buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(ValidationException ex) {
        logger.warn("Validation error: {}", ex.getMessage());
        return new ResponseEntity<>(buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidDataAccessApiUsageException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidDataAccessApiUsageException(InvalidDataAccessApiUsageException ex) {
        logger.warn("Invalid data access API usage: {}", ex.getMessage());
        return new ResponseEntity<>(buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid data access API usage"), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFoundException(UserNotFoundException ex) {
        logger.warn("User not found: {}", ex.getMessage());
        return new ResponseEntity<>(buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ResourcesNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFoundException(ResourcesNotFoundException ex) {
        logger.warn("Resource not found: {}", ex.getMessage());
        return new ResponseEntity<>(buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResourceFoundException(NoResourceFoundException ex) {
        logger.warn("No resource found: {}", ex.getMessage());
        return new ResponseEntity<>(buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException ex) {
        logger.warn("Method not supported: {}", ex.getMessage());
        return new ResponseEntity<>(buildErrorResponse(HttpStatus.METHOD_NOT_ALLOWED, ex.getMessage()), HttpStatus.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        logger.warn("Invalid request body: {}", ex.getMessage());
        return new ResponseEntity<>(buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid request body format"), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        logger.warn("Illegal argument: {}", ex.getMessage());
        return new ResponseEntity<>(buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        Map<String, String> errors = new HashMap<>();
        String field = ex.getName();
        String value = ex.getValue() != null ? ex.getValue().toString() : "null";
        String expectedType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        errors.put(field, String.format("Failed to convert value '%s' to required type '%s'", value, expectedType));
        
        logger.warn("Type mismatch: {}", errors);
        return new ResponseEntity<>(buildErrorResponse(HttpStatus.BAD_REQUEST, "Type mismatch error", errors), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fieldError -> fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "Invalid value",
                        (existing, replacement) -> existing)); // Merge duplicate keys
        
        logger.warn("Validation failed: {}", errors);
        return new ResponseEntity<>(buildErrorResponse(HttpStatus.BAD_REQUEST, "Validation failed", errors), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(AccessDeniedException ex) {
        logger.warn("Access denied: {}", ex.getMessage());
        return new ResponseEntity<>(buildErrorResponse(HttpStatus.FORBIDDEN, "Access denied: You do not have permission to perform this action"), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, Object>> handleSecurityException(SecurityException ex) {
        logger.warn("Security violation: {}", ex.getMessage());
        return new ResponseEntity<>(buildErrorResponse(HttpStatus.FORBIDDEN, ex.getMessage()), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        logger.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        return new ResponseEntity<>(buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred"), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}