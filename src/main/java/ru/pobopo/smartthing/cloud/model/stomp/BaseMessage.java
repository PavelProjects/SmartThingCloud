package ru.pobopo.smartthing.cloud.model.stomp;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class BaseMessage {
    private final GatewayMessageType type;
    private String requestId;
    private boolean cacheable = true;
    private boolean needResponse = true;

    public BaseMessage(GatewayMessageType type) {
        this.type = type;
    }
}
