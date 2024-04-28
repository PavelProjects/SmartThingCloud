package ru.pobopo.smartthing.cloud.controller;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.pobopo.smartthing.cloud.controller.model.ErrorResponse;
import ru.pobopo.smartthing.cloud.exception.AccessDeniedException;
import ru.pobopo.smartthing.cloud.exception.CommandNotAllowed;
import ru.pobopo.smartthing.cloud.exception.GatewayRequestException;
import ru.pobopo.smartthing.cloud.exception.ValidationException;
import ru.pobopo.smartthing.model.stomp.ResponseMessage;

import javax.naming.AuthenticationException;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeoutException;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {

    static{
        Locale.setDefault(new Locale("en"));
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse badToken(MalformedJwtException exc) {
        log.warn(exc.getMessage());
        return new ErrorResponse("Bad token");
    }

    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationException(ValidationException exc) {
        log.warn("Handle ValidationException: {}", exc.getMessage());
        return new ErrorResponse(exc.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse accessDeniedException(AccessDeniedException exc) {
        log.warn(exc.getMessage());
        return new ErrorResponse("Permission denied");
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse authException(AuthenticationException exc) {
        log.warn(exc.getMessage());
        return new ErrorResponse("Current user not authenticated");
    }

    @ExceptionHandler(ExpiredJwtException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse tokenExpired(ExpiredJwtException exc) {
        log.warn(exc.getMessage());
        return new ErrorResponse("Token expired");
    }

    @ExceptionHandler(JwtException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse wrongToken(JwtException exc) {
        log.warn(exc.getMessage());
        return new ErrorResponse("Wrong token");
    }

    @ExceptionHandler(IOException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse ioException(IOException exc) {
        log.warn(exc.getMessage());
        return new ErrorResponse(exc.getMessage(), exc);
    }

    @ExceptionHandler(CommandNotAllowed.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse commandNotAllowed(CommandNotAllowed exception) {
        log.warn(exception.getMessage());
        return new ErrorResponse(exception.getMessage());
    }

    @ExceptionHandler(TimeoutException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse timeout(TimeoutException exception) {
        return new ErrorResponse(exception.getMessage());
    }

    @ExceptionHandler(GatewayRequestException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse gatewayRequestException(GatewayRequestException exception) {
        ResponseMessage message = exception.getResponseMessage();
        return ErrorResponse.builder()
                .message(String.format(
                        "Gateway request (id=%s) failed: %s",
                        message.getRequestId(),
                        message.getError()
                ))
                .stack(message.getStack())
                .build();
    }
}
