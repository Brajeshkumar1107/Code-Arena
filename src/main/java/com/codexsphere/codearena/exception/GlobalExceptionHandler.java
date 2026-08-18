package com.codexsphere.codearena.exception;

import com.codexsphere.codearena.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles all application-specific exceptions.
     *
     * The HTTP status is determined by the exception itself.
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(
            BaseException ex,
            HttpServletRequest request
    ) {

        HttpStatus status =
                ex.getStatus();

        ErrorResponse response =
                ErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(status.value())
                        .error(status.getReasonPhrase())
                        .message(ex.getMessage())
                        .path(request.getRequestURI())
                        .build();

        return ResponseEntity
                .status(status)
                .body(response);
    }

    /**
     * Handles @Valid validation failures.
     */
    @ExceptionHandler(
            MethodArgumentNotValidException.class
    )
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {

        String message =
                ex.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .map(error ->
                                error.getField()
                                        + ": "
                                        + error.getDefaultMessage()
                        )
                        .collect(
                                Collectors.joining(", ")
                        );

        ErrorResponse response =
                ErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(
                                HttpStatus.BAD_REQUEST.value()
                        )
                        .error(
                                HttpStatus.BAD_REQUEST
                                        .getReasonPhrase()
                        )
                        .message(message)
                        .path(request.getRequestURI())
                        .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * Handles malformed JSON request bodies.
     */
    @ExceptionHandler(
            HttpMessageNotReadableException.class
    )
    public ResponseEntity<ErrorResponse> handleInvalidJson(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {

        ErrorResponse response =
                ErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(
                                HttpStatus.BAD_REQUEST.value()
                        )
                        .error(
                                HttpStatus.BAD_REQUEST
                                        .getReasonPhrase()
                        )
                        .message(
                                "Invalid request body."
                        )
                        .path(request.getRequestURI())
                        .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * Handles wrong HTTP method (e.g. GET on a POST endpoint).
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request
    ) {

        ErrorResponse response =
                ErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(
                                HttpStatus.METHOD_NOT_ALLOWED.value()
                        )
                        .error(
                                HttpStatus.METHOD_NOT_ALLOWED
                                        .getReasonPhrase()
                        )
                        .message(ex.getMessage())
                        .path(request.getRequestURI())
                        .build();

        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(response);
    }

    /**
     * Handles unsupported media type (e.g. text/plain on a JSON endpoint).
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex,
            HttpServletRequest request
    ) {

        ErrorResponse response =
                ErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(
                                HttpStatus.UNSUPPORTED_MEDIA_TYPE.value()
                        )
                        .error(
                                HttpStatus.UNSUPPORTED_MEDIA_TYPE
                                        .getReasonPhrase()
                        )
                        .message(ex.getMessage())
                        .path(request.getRequestURI())
                        .build();

        return ResponseEntity
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(response);
    }

    /**
     * Handles unexpected exceptions.
     *
     * Internal exception details are intentionally
     * not exposed to the client.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception ex,
            HttpServletRequest request
    ) {

        ErrorResponse response =
                ErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(
                                HttpStatus.INTERNAL_SERVER_ERROR
                                        .value()
                        )
                        .error(
                                HttpStatus.INTERNAL_SERVER_ERROR
                                        .getReasonPhrase()
                        )
                        .message(
                                "Unexpected error occurred."
                        )
                        .path(request.getRequestURI())
                        .build();

        return ResponseEntity
                .status(
                        HttpStatus.INTERNAL_SERVER_ERROR
                )
                .body(response);
    }
}