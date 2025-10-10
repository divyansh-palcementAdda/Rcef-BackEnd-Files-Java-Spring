package com.renaissance.app.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(EmailAlreadyExistException.class)
	public ResponseEntity<Map<String, Object>> handleEmailAlreadyExistException(EmailAlreadyExistException ex) {
		return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage());
	}
	
	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<Map<String, Object>> handleBadCredentialsException(BadCredentialsException ex) {
		System.err.println(ex.getMessage());
		return buildErrorResponse(HttpStatus.NOT_ACCEPTABLE, "Invalid Email or Password");
	}
	
	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<Map<String, Object>> handleConstraintViolationException(ConstraintViolationException ex) {
		return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage());
	}
	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<Map<String, Object>> handleMissingServletRequestParameterException(MissingServletRequestParameterException ex) {
		return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
	}
	@ExceptionHandler(ValidationException.class)
	public ResponseEntity<Map<String, Object>> handleValidationException(ValidationException ex) {
		return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	@ExceptionHandler(InvalidDataAccessApiUsageException.class)
	public ResponseEntity<Map<String, Object>> handleInvalidDataAccessApiUsageException(
			InvalidDataAccessApiUsageException ex) {
		return buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid data access API usage.");
	}

	@ExceptionHandler(UserNotFoundException.class)
	public ResponseEntity<Map<String, Object>> handleUserNotFoundException(UserNotFoundException ex) {
		return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<Map<String, Object>> handleHttpRequestMethodNotSupportedException(
			HttpRequestMethodNotSupportedException ex) {
		return buildErrorResponse(HttpStatus.METHOD_NOT_ALLOWED, ex.getMessage());
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
		return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
	}
	
	@ExceptionHandler(ResourcesNotFoundException.class)
	public ResponseEntity<Map<String, Object>> handleResourceNotFoundException(ResourcesNotFoundException ex) {
		return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<Map<String, Object>> handleNoResourceFoundException(NoResourceFoundException ex) {
		return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
		return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<Map<String, Object>> handleMethodArgumentTypeMismatchException(
			MethodArgumentTypeMismatchException ex) {
		Map<String, Object> response = new HashMap<>();
		Map<String, String> errors = new HashMap<>();

		String field = ex.getName();
		String value = String.valueOf(ex.getValue());
		String expectedType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";

		errors.put(field, "Failed to convert value '" + value + "' to required type '" + expectedType + "'");

		response.put("timestamp", LocalDateTime.now());
		response.put("status", HttpStatus.BAD_REQUEST.value());
		response.put("error", HttpStatus.BAD_REQUEST.getReasonPhrase());
		response.put("message", "Type mismatch error");
		response.put("errors", errors);

		return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValidException(
			MethodArgumentNotValidException ex) {
		Map<String, String> errors = new HashMap<>();
		for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
			errors.put(fieldError.getField(), fieldError.getDefaultMessage());
		}

		Map<String, Object> response = new HashMap<>();
		response.put("timestamp", LocalDateTime.now());
		response.put("status", HttpStatus.BAD_REQUEST.value());
		response.put("error", HttpStatus.BAD_REQUEST.getReasonPhrase());
		response.put("message", "Validation failed");
		response.put("errors", errors);

		return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
		return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
	}

	private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String message) {
		Map<String, Object> error = new HashMap<>();
		error.put("timestamp", LocalDateTime.now());
		error.put("status", status.value());
		error.put("error", status.getReasonPhrase());
		error.put("message", message);

		return new ResponseEntity<>(error, status);
	}

}

