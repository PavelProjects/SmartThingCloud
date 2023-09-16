package ru.pobopo.smartthing.cloud.context;

import ru.pobopo.smartthing.cloud.entity.GatewayEntity;

public class ContextHolder {
    private static GatewayEntity gatewayEntity;

    public static void clearContext() {
        ContextHolder.gatewayEntity = null;
    }

    public static void setCurrentGateway(GatewayEntity gatewayEntity) {
        ContextHolder.gatewayEntity = gatewayEntity;
    }

    public static GatewayEntity getCurrentGateway() {
        return ContextHolder.gatewayEntity;
    }
}
