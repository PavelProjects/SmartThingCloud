package ru.pobopo.smartthing.cloud.controller.model;

import lombok.Data;
import ru.pobopo.smartthing.cloud.model.stomp.GatewayCommandMessage;

import java.util.Map;

@Data
public class SendCommandRequest{
    private String gatewayId;
    private String command;
    private Map<String, Object> parameters;

    public GatewayCommandMessage toGatewayCommand() {
        return new GatewayCommandMessage(command, parameters);
    }
}
