package ru.pobopo.smartthing.cloud.controller.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Класс для отправки юзеру данных об произошедшей ошибке
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private String message;
    private Exception exception;

    public ErrorResponse(String message) {
        this.message = message;
    }
}
