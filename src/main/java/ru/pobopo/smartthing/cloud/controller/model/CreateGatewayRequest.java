package ru.pobopo.smartthing.cloud.controller.model;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateGatewayRequest {
    @NotNull
    @NotEmpty
    private String name;
    private String description;
}
