package ru.pobopo.smartthing.cloud.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Класс для отправки юзеру данных об произошедшей ошибке
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private String message;
    private String stack;
    private Exception exception;

    public ErrorResponse(String message) {
        this.message = message;
    }

    public ErrorResponse(String message, Exception exception) {
        this.message = message;
        this.exception = exception;
    }
}
