package ru.pobopo.smartthing.cloud.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GatewayDto {
    private String id;
    private String name;
    private String description;
    private Boolean online;
    private Boolean haveToken;
}
