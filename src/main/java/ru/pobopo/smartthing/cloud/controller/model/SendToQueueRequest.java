package ru.pobopo.smartthing.cloud.controller.model;

import lombok.Data;

@Data
public class SendToQueueRequest {
    private String gatewayId;
    private String message;
}
