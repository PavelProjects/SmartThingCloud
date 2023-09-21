package ru.pobopo.smartthing.cloud.controller.model;

import java.util.Map;
import lombok.Data;
import ru.pobopo.smartthing.cloud.rabbitmq.GatewayCommand;

@Data
public class SendCommandRequest{
    private String gatewayId;
    private String command;
    private Map<String, Object> parameters;

    public GatewayCommand toGatewayCommand() {
        return new GatewayCommand(command, parameters);
    }
}
