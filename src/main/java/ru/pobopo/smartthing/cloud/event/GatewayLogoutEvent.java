package ru.pobopo.smartthing.cloud.event;

import org.springframework.context.ApplicationEvent;
import ru.pobopo.smartthing.cloud.entity.GatewayEntity;

public class GatewayLogoutEvent extends ApplicationEvent {
    private final GatewayEntity gateway;

    public GatewayLogoutEvent(Object source, GatewayEntity gateway) {
        super(source);
        this.gateway = gateway;
    }

    public GatewayEntity getGateway() {
        return gateway;
    }
}