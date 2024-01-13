package ru.pobopo.smartthing.cloud.dto;

import lombok.Data;

@Data
public class GatewayDto {
    private String id;
    private String name;
    private String description;
    private boolean online;
    private boolean haveToken;
}
