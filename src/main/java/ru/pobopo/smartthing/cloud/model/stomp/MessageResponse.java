package ru.pobopo.smartthing.cloud.model.stomp;

import lombok.Data;

@Data
public class MessageResponse {
    private String requestId;
    private String response;
    private boolean success = true;
}
