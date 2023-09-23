package ru.pobopo.smartthing.cloud.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class GatewayRequestDto {
    private String id;
    private LocalDateTime sentDate;
    private LocalDateTime receiveDate;
    private String message;
    private String result;
    private boolean finished;
    private boolean success;
    private GatewayDto gateway;
    private UserDto user;
}
