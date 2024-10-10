package ru.pobopo.smartthing.cloud.controller.dto;

import lombok.Data;

@Data
public class AuthorizedUserDto {
    private UserDto user;
    private GatewayDto gateway;
}
