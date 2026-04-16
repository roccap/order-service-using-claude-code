package com.skmcore.orderservice.dto;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        int status,
        String error,
        String message,
        String path,
        Instant timestamp,
        List<FieldValidationError> errors,
        String correlationId
) {}
