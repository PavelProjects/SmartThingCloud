package ru.pobopo.smartthing.cloud.stomp;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;

@Getter
@Setter
@ToString(callSuper = true)
public class GatewayCommandMessage extends BaseMessage {
    private String command;
    private Map<String, Object> parameters;

    public GatewayCommandMessage() {
        super(GatewayMessageType.GATEWAY_COMMAND);
    }

    public GatewayCommandMessage(String command, Map<String, Object> parameters) {
        super(GatewayMessageType.GATEWAY_COMMAND);
        this.command = command;
        this.parameters = parameters;
    }
}
