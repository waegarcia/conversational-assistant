package com.enterprise.assistant.api;

import com.enterprise.assistant.api.dto.ErrorResponse;
import com.enterprise.assistant.infrastructure.external.ExternalServiceException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(buildError("Validation Error", errors, 400));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(buildError("Bad Request", ex.getMessage(), 400));
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ErrorResponse> handleExternalService(ExternalServiceException ex) {
        return ResponseEntity.status(503).body(buildError("External Service Error", 
                "No se pudo conectar con el servicio externo", 503));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        return ResponseEntity.status(500).body(buildError("Internal Error", 
                "Ocurrio un error inesperado", 500));
    }

    private ErrorResponse buildError(String error, String message, int status) {
        return ErrorResponse.builder()
                .error(error)
                .message(message)
                .status(status)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
