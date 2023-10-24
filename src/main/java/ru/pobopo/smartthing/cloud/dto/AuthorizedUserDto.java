package ru.pobopo.smartthing.cloud.dto;

import lombok.Data;

@Data
public class AuthorizedUserDto {
    private UserDto user;
    private GatewayDto gateway;
}
