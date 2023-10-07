package ru.pobopo.smartthing.cloud.rabbitmq;

import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.Nullable;

public enum GatewayMessageType {
    DEVICE_REQUEST("device_request"),
    GATEWAY_COMMAND("gateway_command");

    private String type;
    GatewayMessageType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    @Nullable
    public GatewayMessageType fromValue(String type) {
        if (StringUtils.isBlank(type)) {
            return null;
        }

        for (GatewayMessageType commandType: values()) {
            if (StringUtils.equals(commandType.getType(), type)) {
                return commandType;
            }
        }

        return null;
    }
}
