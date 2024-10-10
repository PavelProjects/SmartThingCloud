package ru.pobopo.smartthing.cloud.controller.dto;

import lombok.Data;

@Data
public class GatewayConfigDto {
    private String brokerIp;
    private int brokerPort;
    private String queueIn;
    private String queueOut;
}
