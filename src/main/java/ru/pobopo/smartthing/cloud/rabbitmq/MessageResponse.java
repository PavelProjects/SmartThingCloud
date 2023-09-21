package ru.pobopo.smartthing.cloud.rabbitmq;

import lombok.Data;

@Data
public class MessageResponse {
    private String requestId;
    private String response;
    private boolean success = true;
}
