package ru.pobopo.smartthing.cloud.model.stomp;

import lombok.Data;

@Data
public class MessageResponse {
    private String requestId;
    private Object response;
    private String error;
    private boolean success = true;
}
