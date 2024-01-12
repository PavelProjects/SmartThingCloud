package ru.pobopo.smartthing.cloud.model.stomp;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.Nullable;

@Getter
public enum GatewayCommand {
    LOGOUT("logout"),
    PING("ping");

    private final String name;
    GatewayCommand(String name) {
        this.name = name;
    }

    @Nullable
    public GatewayCommand fromValue(String type) {
        if (StringUtils.isBlank(type)) {
            return null;
        }

        for (GatewayCommand commandType: values()) {
            if (StringUtils.equals(commandType.getName(), type)) {
                return commandType;
            }
        }

        return null;
    }
}
