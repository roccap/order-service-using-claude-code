package com.skmcore.orderservice.exception;

import com.skmcore.orderservice.dto.ErrorResponse;
import com.skmcore.orderservice.dto.FieldValidationError;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String MDC_CORRELATION_ID = "correlationId";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String correlationId = resolveCorrelationId(request);
        List<FieldValidationError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldValidationError(
                        fe.getField(),
                        fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value"))
                .toList();
        log.warn("Validation failed on {}: {} (correlationId={})",
                request.getRequestURI(), errors, correlationId);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(error(400, "Validation Failed", "Request validation failed",
                        request, errors, correlationId));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(
            EntityNotFoundException ex, HttpServletRequest request) {
        String correlationId = resolveCorrelationId(request);
        log.warn("Entity not found: {} (correlationId={})", ex.getMessage(), correlationId);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(error(404, "Not Found", ex.getMessage(), request, null, correlationId));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(
            IllegalStateException ex, HttpServletRequest request) {
        String correlationId = resolveCorrelationId(request);
        log.warn("Illegal state transition: {} (correlationId={})", ex.getMessage(), correlationId);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(error(409, "Conflict", ex.getMessage(), request, null, correlationId));
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLocking(
            OptimisticLockingFailureException ex, HttpServletRequest request) {
        String correlationId = resolveCorrelationId(request);
        log.warn("Optimistic locking conflict on {}: {} (correlationId={})",
                request.getRequestURI(), ex.getMessage(), correlationId);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(error(409, "Conflict",
                        "The resource was modified by another request. Please retry.",
                        request, null, correlationId));
    }

    @ExceptionHandler(OrderAlreadyCancelledException.class)
    public ResponseEntity<ErrorResponse> handleAlreadyCancelled(
            OrderAlreadyCancelledException ex, HttpServletRequest request) {
        String correlationId = resolveCorrelationId(request);
        log.warn("Order already cancelled: {} (correlationId={})", ex.getMessage(), correlationId);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(error(409, "Conflict", ex.getMessage(), request, null, correlationId));
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmail(
            DuplicateEmailException ex, HttpServletRequest request) {
        String correlationId = resolveCorrelationId(request);
        log.warn("Duplicate email: {} (correlationId={})", ex.getMessage(), correlationId);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(error(409, "Conflict", ex.getMessage(), request, null, correlationId));
    }

    @ExceptionHandler(InvalidOrderStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidState(
            InvalidOrderStateException ex, HttpServletRequest request) {
        String correlationId = resolveCorrelationId(request);
        log.warn("Invalid order state transition: {} (correlationId={})", ex.getMessage(), correlationId);
        return ResponseEntity.status(422)
                .body(error(422, "Unprocessable Entity", ex.getMessage(), request, null, correlationId));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex, HttpServletRequest request) {
        String correlationId = resolveCorrelationId(request);
        log.error("Unexpected error on {} (correlationId={})", request.getRequestURI(), correlationId, ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error(500, "Internal Server Error", "An unexpected error occurred",
                        request, null, correlationId));
    }

    // --- helpers ---

    private String resolveCorrelationId(HttpServletRequest request) {
        String header = request.getHeader(CORRELATION_ID_HEADER);
        String correlationId = (header != null && !header.isBlank()) ? header : UUID.randomUUID().toString();
        MDC.put(MDC_CORRELATION_ID, correlationId);
        return correlationId;
    }

    private ErrorResponse error(int status, String errorLabel, String message,
                                HttpServletRequest request, List<FieldValidationError> errors,
                                String correlationId) {
        return new ErrorResponse(status, errorLabel, message,
                request.getRequestURI(), Instant.now(), errors, correlationId);
    }
}
