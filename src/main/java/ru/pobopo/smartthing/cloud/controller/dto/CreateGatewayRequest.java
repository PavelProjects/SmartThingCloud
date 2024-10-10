package ru.pobopo.smartthing.cloud.controller.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateGatewayRequest {
    @NotNull
    @NotEmpty
    private String name;
    private String description;
}
